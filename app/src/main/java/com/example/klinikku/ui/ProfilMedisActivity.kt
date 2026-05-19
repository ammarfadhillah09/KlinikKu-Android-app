package com.example.klinikku.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.model.ProfilMedis
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfilMedisActivity : AppCompatActivity() {

    private lateinit var spinnerGolonganDarah: Spinner
    private lateinit var etRiwayatPenyakit: EditText
    private lateinit var etAlergiObat: EditText
    private lateinit var btnSimpan: Button
    private var userNik: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil_medis)

        // Initialize UI
        spinnerGolonganDarah = findViewById(R.id.spinnerGolonganDarah)
        etRiwayatPenyakit = findViewById(R.id.etRiwayatPenyakit)
        etAlergiObat = findViewById(R.id.etAlergiObat)
        btnSimpan = findViewById(R.id.btnSimpanProfilMedis)

        // 1. Ambil NIK dari SharedPreferences
        val sessionManager = SessionManager(this)
        userNik = sessionManager.getNik()

        if (userNik.isNullOrEmpty()) {
            Toast.makeText(this, "Sesi habis, silakan login kembali", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Muat Data Profil Medis dari API
        muatProfilMedis(userNik!!)

        // 3. Logika Simpan
        btnSimpan.setOnClickListener {
            simpanProfilMedis()
        }
    }

    private fun muatProfilMedis(nik: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.getProfilMedis(nik)
                if (response.isSuccessful && response.body()?.data != null) {
                    val data = response.body()?.data
                    populateForm(data!!)
                }
            } catch (e: Exception) {
                Log.e("PROFIL_MEDIS", "Gagal memuat: ${e.message}")
            }
        }
    }

    private fun populateForm(data: ProfilMedis) {
        etRiwayatPenyakit.setText(data.riwayat_penyakit)
        etAlergiObat.setText(data.alergi_obat)

        // Set Spinner Selection
        val adapter = spinnerGolonganDarah.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == data.golongan_darah) {
                spinnerGolonganDarah.setSelection(i)
                break
            }
        }
    }

    private fun simpanProfilMedis() {
        val golDarah = spinnerGolonganDarah.selectedItem.toString()
        val riwayat = etRiwayatPenyakit.text.toString().trim()
        val alergi = etAlergiObat.text.toString().trim()

        if (golDarah == "Pilih Golongan Darah") {
            Toast.makeText(this, "Pilih golongan darah terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val profilBaru = ProfilMedis(
            nik = userNik!!,
            golongan_darah = golDarah,
            riwayat_penyakit = riwayat,
            alergi_obat = alergi
        )

        btnSimpan.isEnabled = false
        btnSimpan.text = "Menyimpan..."

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.saveProfilMedis(profilBaru)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfilMedisActivity, "Profil Medis Berhasil Diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ProfilMedisActivity, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfilMedisActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSimpan.isEnabled = true
                btnSimpan.text = "SIMPAN PERUBAHAN"
            }
        }
    }
}
