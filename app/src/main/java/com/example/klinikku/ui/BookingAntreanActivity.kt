package com.example.klinikku.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.*

/**
 * BookingAntreanActivity
 *
 * Step 1 of the patient booking flow:
 *   - Shows patient name & NIK
 *   - Patient selects target Poli (loaded dynamically from API)
 *   - Patient picks visit date via DatePickerDialog
 *   - Taps "Pilih Dokter" → navigates to PendaftaranKlinikActivity
 *     where they can see the real-time doctor list with schedules
 */
class BookingAntreanActivity : AppCompatActivity() {

    private lateinit var tvNama: TextView
    private lateinit var tvNik: TextView
    private lateinit var spinnerPoli: Spinner
    private lateinit var etTanggal: EditText
    private lateinit var btnPilihDokter: Button

    private var userNik: String? = null
    private var userNama: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_antrean)

        // Initialize UI
        tvNama = findViewById(R.id.tvBookingNama)
        tvNik = findViewById(R.id.tvBookingNik)
        spinnerPoli = findViewById(R.id.spinnerPoli)
        etTanggal = findViewById(R.id.etTanggalKunjungan)
        btnPilihDokter = findViewById(R.id.btnAmbilAntrean)

        // Update button label to match the new 2-step flow
        btnPilihDokter.text = "PILIH DOKTER"

        // 1. Read patient data from SharedPreferences
        val sessionManager = SessionManager(this)
        userNik = sessionManager.getNik()
        userNama = sessionManager.getNama() ?: "Pasien"

        tvNama.text = "Nama: $userNama"
        tvNik.text = "NIK: $userNik"

        // 2. Setup DatePickerDialog
        etTanggal.setOnClickListener { showDatePicker() }

        // 3. Load poli list dynamically from API
        loadPoliSpinner()

        // 4. Check Quota in Firebase before navigating to doctor listing screen
        // Pastikan hanya ini satu-satunya OnClickListener untuk btnPilihDokter
        btnPilihDokter.setOnClickListener { checkQuotaSebelumDaftar() }
    }

    private fun loadPoliSpinner() {
        lifecycleScope.launch {
            val poliNames = try {
                val response = RetrofitClient.instance.getPoli()
                if (response.isSuccessful) {
                    val names = response.body()?.data?.map { it.nama_poli } ?: emptyList()
                    names.ifEmpty { listOf("Umum", "Gigi", "Anak") }
                } else {
                    listOf("Umum", "Gigi", "Anak")
                }
            } catch (e: Exception) {
                Log.e("BOOKING_ANTREAN", "Gagal memuat poli: ${e.message}")
                listOf("Umum", "Gigi", "Anak")
            }

            val adapter = ArrayAdapter(
                this@BookingAntreanActivity,
                android.R.layout.simple_spinner_item,
                poliNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPoli.adapter = adapter
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                // Format as YYYY-MM-DD with zero-padded month/day
                val tanggal = "$year-${String.format("%02d", month + 1)}-${String.format("%02d", day)}"
                etTanggal.setText(tanggal)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun checkQuotaSebelumDaftar() {
        val poli = spinnerPoli.selectedItem?.toString() ?: ""
        val tanggalInputStr = etTanggal.text.toString().trim()

        if (poli.isBlank()) {
            Toast.makeText(this, "Silakan pilih poli tujuan", Toast.LENGTH_SHORT).show()
            return
        }
        if (tanggalInputStr.isEmpty()) {
            Toast.makeText(this, "Silakan pilih tanggal kunjungan", Toast.LENGTH_SHORT).show()
            return
        }

        if (userNik == null) {
            Toast.makeText(this, "Sesi tidak valid, NIK kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val antreanRef = database.getReference("antrean")

        btnPilihDokter.isEnabled = false
        btnPilihDokter.text = "Mengecek Kuota..."

        antreanRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                var count = 0
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

                try {
                    val inputDate = sdf.parse(tanggalInputStr) ?: java.util.Date()
                    val inputCalendar = java.util.Calendar.getInstance().apply { time = inputDate }
                    val inputWeek = inputCalendar.get(java.util.Calendar.WEEK_OF_YEAR)
                    val inputYear = inputCalendar.get(java.util.Calendar.YEAR)

                    for (data in snapshot.children) {
                        val pendaftaranId = data.child("pasien_id").getValue(String::class.java)
                        val pendaftaranTgl = data.child("tanggal_kunjungan").getValue(String::class.java)
                        val pendaftaranStatus = data.child("status").getValue(String::class.java) ?: "Menunggu" // Beri default value jika null!

                        // Pastikan pengecekan id dan tanggal aman dari null
                        if (pendaftaranId == userNik && pendaftaranTgl != null) {
                            // Gunakan fungsi safe call atau pastikan pendaftaranStatus tidak null sebelum equals
                            if (!pendaftaranStatus.equals("dibatalkan", ignoreCase = true)) {
                                val dbDate = sdf.parse(pendaftaranTgl)
                                if (dbDate != null) {
                                    val dbCalendar = java.util.Calendar.getInstance().apply { time = dbDate }
                                    val dbWeek = dbCalendar.get(java.util.Calendar.WEEK_OF_YEAR)
                                    val dbYear = dbCalendar.get(java.util.Calendar.YEAR)

                                    if (dbWeek == inputWeek && dbYear == inputYear) {
                                        count++
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Abaikan error parsing tanggal
                }

                btnPilihDokter.isEnabled = true
                btnPilihDokter.text = "PILIH DOKTER"

                // Batasan kuota murni dikontrol oleh kondisi ini (4 kali)
                if (count >= 4) {
                    Toast.makeText(this@BookingAntreanActivity, "Maaf, Anda telah mencapai batas maksimal pendaftaran (4 kali dalam seminggu)!", Toast.LENGTH_LONG).show()
                } else {
                    navigateToPendaftaranLanjut(poli, tanggalInputStr)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                btnPilihDokter.isEnabled = true
                btnPilihDokter.text = "PILIH DOKTER"
                Toast.makeText(this@BookingAntreanActivity, "Gagal mengecek kuota: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun navigateToPendaftaranLanjut(poli: String, tanggal: String) {
        val intent = Intent(this, PendaftaranKlinikActivity::class.java).apply {
            putExtra(PendaftaranKlinikActivity.EXTRA_POLI_NAMA, poli)
            putExtra(PendaftaranKlinikActivity.EXTRA_TANGGAL, tanggal)
        }
        startActivity(intent)
    }
}
