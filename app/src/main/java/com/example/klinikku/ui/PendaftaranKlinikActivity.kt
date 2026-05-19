package com.example.klinikku.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.model.Dokter
import com.example.klinikku.data.model.PendaftaranRequest
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.*

/**
 * PendaftaranKlinikActivity
 *
 * Displays the list of active doctors in the selected Poli.
 * Each doctor card shows:
 *  - Name & initials avatar
 *  - Specialist info
 *  - Schedule: "Jadwal: Senin, Selasa (08:00 - 17:00)"
 *  - Today's practice status badge
 *  - "Daftar Sekarang" button with optional schedule-day validation
 *
 * Receives extras from BookingAntreanActivity:
 *  - EXTRA_POLI_NAMA  : String  – selected poli name
 *  - EXTRA_TANGGAL    : String  – selected visit date
 */
class PendaftaranKlinikActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_POLI_NAMA = "EXTRA_POLI_NAMA"
        const val EXTRA_TANGGAL = "EXTRA_TANGGAL"
    }

    // Indonesian day names aligned to Calendar.DAY_OF_WEEK (1=Sunday … 7=Saturday)
    private val hariIndonesia = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")

    private lateinit var rvDokter: RecyclerView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvSubtitlePoli: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var adapter: DokterPasienAdapter

    private var poliNama: String = ""
    private var tanggal: String = ""
    private var userNik: String = ""
    private var userNama: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pendaftaran_klinik)

        // ── 1. Read Intent Extras ──────────────────────────────────────────────
        poliNama = intent.getStringExtra(EXTRA_POLI_NAMA) ?: ""
        tanggal = intent.getStringExtra(EXTRA_TANGGAL) ?: ""

        // ── 2. Read Patient Data from SharedPreferences ────────────────────────
        val sessionManager = SessionManager(this)
        userNik = sessionManager.getNik() ?: ""
        userNama = sessionManager.getNama() ?: "Pasien"

        // ── 3. Bind Views ──────────────────────────────────────────────────────
        rvDokter = findViewById(R.id.rvDokterPasien)
        layoutLoading = findViewById(R.id.layoutLoading)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvSubtitlePoli = findViewById(R.id.tvSubtitlePoli)
        tvTanggal = findViewById(R.id.tvTanggalPendaftaran)
        val btnBack = findViewById<View>(R.id.btnBackPendaftaran)

        // ── 4. Set Header Info ─────────────────────────────────────────────────
        tvSubtitlePoli.text = "Poli $poliNama"
        tvTanggal.text = "Tanggal Kunjungan: $tanggal"

        btnBack.setOnClickListener { finish() }

        // ── 5. Setup RecyclerView & Adapter ───────────────────────────────────
        val targetDayRaw = getNamaHariDariTanggal(tanggal)
        val targetBookingDayClean = targetDayRaw.trim().lowercase()
        adapter = DokterPasienAdapter(listOf(), targetBookingDayClean) { dokter ->
            handleDaftarClick(dokter)
        }
        rvDokter.layoutManager = LinearLayoutManager(this)
        rvDokter.adapter = adapter

        // ── 6. Fetch Doctor Data ───────────────────────────────────────────────
        muatDokterByPoli()
    }

    /**
     * Fetches ALL doctors from the API, then filters by the selected poli name.
     * The Dokter object already contains: hari, jam_mulai, jam_selesai, id_jadwal
     * (embedded by the unified backend from the linked schedule).
     */
    private fun muatDokterByPoli() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getDokter()
                if (response.isSuccessful) {
                    val allDokter: List<Dokter> = response.body()?.data ?: emptyList()

                    // Filter by selected poli (case-insensitive match)
                    val filteredDokter = allDokter.filter { dokter ->
                        dokter.poli.trim().equals(poliNama.trim(), ignoreCase = true)
                    }

                    showLoading(false)
                    if (filteredDokter.isEmpty()) {
                        showEmpty(true)
                    } else {
                        showEmpty(false)
                        rvDokter.visibility = View.VISIBLE
                        
                        // Strict data and state validation before adapter attach
                        val targetDayRaw = getNamaHariDariTanggal(tanggal)
                        val targetBookingDayClean = targetDayRaw.trim().lowercase()
                        
                        Log.d("DEBUG_BOOKING", "Target Day: \$targetBookingDayClean, Total Doctors: \${filteredDokter.size}")
                        
                        // Option A: Re-initialize the adapter with the real data
                        adapter = DokterPasienAdapter(filteredDokter, targetBookingDayClean) { dokter ->
                            handleDaftarClick(dokter)
                        }
                        rvDokter.adapter = adapter
                    }
                } else {
                    showLoading(false)
                    showEmpty(true)
                    val errMsg = response.errorBody()?.string() ?: "Gagal memuat dokter"
                    Log.e("PENDAFTARAN_KLINIK", "API Error: $errMsg")
                    Toast.makeText(this@PendaftaranKlinikActivity, "Gagal memuat data dokter", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                showEmpty(true)
                Log.e("PENDAFTARAN_KLINIK", "Exception: ${e.message}")
                Toast.makeText(this@PendaftaranKlinikActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handles "Daftar Sekarang" button tap on a doctor card.
     *
     * UX Validation:
     *   - Check if today's day is in the doctor's hari schedule.
     *   - If NOT → show warning dialog ("Dokter tidak praktek hari ini, tetap mendaftar?")
     *   - If YES  → confirm and proceed to booking directly.
     */
    private fun handleDaftarClick(dokter: Dokter) {
        // Guard: ensure id_jadwal is present
        if (dokter.id_jadwal.isBlank()) {
            Toast.makeText(this, "Jadwal dokter belum tersedia, silakan hubungi admin.", Toast.LENGTH_LONG).show()
            return
        }

        // Get the target booking day from the chosen visit date string
        val targetBookingDay = getNamaHariDariTanggal(tanggal)

        // Parse the doctor's practice days (comma-separated, e.g. "Senin,Rabu,Jumat")
        val daysList = try {
            dokter.hari.split(",").map { it.trim().lowercase() }
        } catch (e: Exception) {
            emptyList()
        }

        val isPracticingOnTargetDay = daysList.contains(targetBookingDay.trim().lowercase())

        if (!isPracticingOnTargetDay) {
            // ── Show warning dialog ────────────────────────────────────────────
            AlertDialog.Builder(this)
                .setTitle("⚠️ Dokter Tidak Praktek Pada Hari Kunjungan")
                .setMessage(
                    "Dr. ${dokter.nama} tidak praktek pada hari $targetBookingDay (Hari Kunjungan Terpilih).\n\n" +
                    "Jadwal Praktek: ${dokter.hari.ifBlank { "-" }}\n\n" +
                    "Apakah Anda tetap ingin mendaftar?"
                )
                .setPositiveButton("Ya, Tetap Daftar") { _, _ ->
                    prosesBooking(dokter)
                }
                .setNegativeButton("Batal", null)
                .show()
        } else {
            // ── Direct confirmation ────────────────────────────────────────────
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi Pendaftaran")
                .setMessage(
                    "Daftar ke:\n\n" +
                    "Dokter  : ${dokter.nama}\n" +
                    "Poli    : $poliNama\n" +
                    "Tanggal : $tanggal\n\n" +
                    "Jadwal  : ${dokter.hari} (${dokter.jam_mulai} - ${dokter.jam_selesai})"
                )
                .setPositiveButton("Daftar") { _, _ ->
                    prosesBooking(dokter)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    /**
     * Sends the booking (pendaftaran) request to the API.
     */
    private fun prosesBooking(dokter: Dokter) {
        val request = PendaftaranRequest(
            pasien_id = userNik,
            nama_pasien = userNama,
            poli = poliNama,
            tanggal_kunjungan = tanggal,
            id_jadwal = dokter.id_jadwal
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.daftarAntrean(request)
                if (response.isSuccessful && response.body() != null) {
                    val nomorAntrean = response.body()?.data?.nomor_antrean ?: 0
                    showSuccessDialog(nomorAntrean, dokter.nama, poliNama, tanggal)
                } else {
                    val rawError = response.errorBody()?.string()
                    var cleanMsg = "Gagal mendaftar antrean"
                    if (!rawError.isNullOrBlank()) {
                        try {
                            val jsonObject = org.json.JSONObject(rawError)
                            if (jsonObject.has("message")) {
                                cleanMsg = jsonObject.getString("message")
                            }
                        } catch (e: Exception) {
                            Log.e("PENDAFTARAN_KLINIK", "Error parsing error body: ${e.message}")
                        }
                    }
                    Log.e("PENDAFTARAN_KLINIK", "Booking failed: $cleanMsg")
                    Toast.makeText(this@PendaftaranKlinikActivity, cleanMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("PENDAFTARAN_KLINIK", "Booking exception: ${e.message}")
                Toast.makeText(this@PendaftaranKlinikActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSuccessDialog(nomor: Int, namaDokter: String, poli: String, tgl: String) {
        AlertDialog.Builder(this)
            .setTitle("🎉 Pendaftaran Berhasil!")
            .setMessage(
                "Anda telah terdaftar!\n\n" +
                "Dokter  : $namaDokter\n" +
                "Poli    : $poli\n" +
                "Tanggal : $tgl\n\n" +
                "Nomor Antrean Anda:\n\n$nomor"
            )
            .setPositiveButton("Selesai") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    // ── View Helpers ──────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            rvDokter.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showEmpty(show: Boolean) {
        layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        if (show) rvDokter.visibility = View.GONE
    }

    /**
     * getNamaHariDariTanggal
     *
     * Converts a "yyyy-MM-dd" date string to an Indonesian day name
     * (e.g. "2026-05-19" -> "Selasa").
     */
    private fun getNamaHariDariTanggal(dateString: String): String {
        return try {
            val sdfInput = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = sdfInput.parse(dateString)
            val sdfOutput = java.text.SimpleDateFormat("EEEE", java.util.Locale.forLanguageTag("id"))
            sdfOutput.format(date ?: java.util.Date())
        } catch (e: Exception) {
            "Senin" // fallback safely
        }
    }
}
