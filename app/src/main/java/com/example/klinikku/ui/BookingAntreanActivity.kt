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

        // 4. Navigate to doctor listing screen
        btnPilihDokter.setOnClickListener { navigateToPendaftaran() }
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

    private fun navigateToPendaftaran() {
        val poli = spinnerPoli.selectedItem?.toString() ?: ""
        val tanggal = etTanggal.text.toString().trim()

        if (poli.isBlank()) {
            Toast.makeText(this, "Silakan pilih poli tujuan", Toast.LENGTH_SHORT).show()
            return
        }
        if (tanggal.isEmpty()) {
            Toast.makeText(this, "Silakan pilih tanggal kunjungan", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, PendaftaranKlinikActivity::class.java).apply {
            putExtra(PendaftaranKlinikActivity.EXTRA_POLI_NAMA, poli)
            putExtra(PendaftaranKlinikActivity.EXTRA_TANGGAL, tanggal)
        }
        startActivity(intent)
    }
}
