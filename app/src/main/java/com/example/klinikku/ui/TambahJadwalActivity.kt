package com.example.klinikku.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.model.Jadwal
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TambahJadwalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_jadwal)

        // ── Bind all form fields ──────────────────────────────────────────────
        val etNik     = findViewById<EditText>(R.id.etNikDokter)
        val etNama    = findViewById<EditText>(R.id.etNamaDokter)
        val etHari    = findViewById<EditText>(R.id.etHariPraktek)
        val etMulai   = findViewById<EditText>(R.id.etJamMulai)
        val etSelesai = findViewById<EditText>(R.id.etJamSelesai)
        val etKuota   = findViewById<EditText>(R.id.etKuota)
        val btnSimpan = findViewById<Button>(R.id.btnSimpanJadwal)

        btnSimpan.setOnClickListener {
            // ── 1. Collect all inputs ─────────────────────────────────────────
            val idDokter  = etNik.text.toString().trim()     // NIK dipakai sebagai id_dokter
            val nama      = etNama.text.toString().trim()
            val hari      = etHari.text.toString().trim()
            val jamMulai  = etMulai.text.toString().trim()
            val jamSelesai = etSelesai.text.toString().trim()
            val kuotaStr  = etKuota.text.toString().trim()

            // ── 2. Validation: semua field wajib diisi ────────────────────────
            if (idDokter.isEmpty() || nama.isEmpty() || hari.isEmpty() ||
                jamMulai.isEmpty() || jamSelesai.isEmpty() || kuotaStr.isEmpty()
            ) {
                Toast.makeText(this, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ── 3. Build Jadwal object ────────────────────────────────────────
            // id_jadwal dibiarkan null — backend akan generate menggunakan Firebase push key
            val jadwalBaru = Jadwal(
                id_jadwal   = null,
                nik_dokter  = idDokter,
                nama_dokter = nama,
                hari        = hari,
                jam_mulai   = jamMulai,
                jam_selesai = jamSelesai,
                kuota       = kuotaStr.toInt()
            )

            simpanJadwal(jadwalBaru, btnSimpan)
        }
    }

    // ── Network call with full try-catch to prevent crashes ───────────────────
    private fun simpanJadwal(jadwal: Jadwal, btn: Button) {
        btn.isEnabled = false
        btn.text = "Menyimpan..."

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.addJadwal(jadwal)

                if (response.isSuccessful) {
                    // ── a) Sukses ─────────────────────────────────────────────
                    Toast.makeText(
                        this@TambahJadwalActivity,
                        "Jadwal Berhasil Ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    // ── b) Server mengembalikan error ─────────────────────────
                    val errorMsg = response.errorBody()?.string()
                        ?: response.message()
                        ?: "Terjadi kesalahan tidak dikenal"
                    Toast.makeText(
                        this@TambahJadwalActivity,
                        "Gagal: $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // ── c) Network / parsing error ────────────────────────────────
                Toast.makeText(
                    this@TambahJadwalActivity,
                    "Koneksi Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btn.isEnabled = true
                btn.text = "SIMPAN JADWAL"
            }
        }
    }
}