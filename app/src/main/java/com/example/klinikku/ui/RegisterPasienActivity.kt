package com.example.klinikku.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.model.CekNikRequest
import com.example.klinikku.data.model.Pasien
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class RegisterPasienActivity : AppCompatActivity() {

    private var isAdminMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_pasien)

        val tvTitle = findViewById<TextView>(R.id.tvTitleRegister)
        val etNik = findViewById<EditText>(R.id.etNikPasien)
        val etNama = findViewById<EditText>(R.id.etNamaPasien)
        val etUser = findViewById<EditText>(R.id.etUsernameBaru)
        val etPass = findViewById<EditText>(R.id.etPasswordBaru)
        val etDomisili = findViewById<EditText>(R.id.etDomisili)
        val btnSimpan = findViewById<Button>(R.id.btnSimpanPasien)
        val layoutForm = findViewById<LinearLayout>(R.id.layoutFormPendaftaran)

        // 1. Mode ditentukan dari Intent Extra (Default: Pasien Mode)
        isAdminMode = intent.getBooleanExtra("is_admin_mode", false)

        // 2. Konfigurasi UI berdasarkan Mode
        if (isAdminMode) {
            // MODE ADMIN: Tambah Master (Form Ringkas)
            tvTitle.text = "Tambah Data Master Pasien"
            etUser.visibility = View.GONE
            etPass.visibility = View.GONE
            etDomisili.visibility = View.GONE
            etNama.isEnabled = true // Admin boleh ketik nama baru
            etNama.setBackgroundResource(R.drawable.bg_input_field)
            layoutForm.visibility = View.VISIBLE
            btnSimpan.text = "SIMPAN DATA MASTER"
        } else {
            // MODE PASIEN: Registrasi Mandiri (Form Lengkap)
            tvTitle.text = "Buat Akun Pasien"
            etNama.isEnabled = false // LOCK: Nama hanya dari server via Cek NIK
            etNama.setBackgroundResource(R.drawable.bg_input_field_disabled)
            layoutForm.visibility = View.GONE
            btnSimpan.text = "DAFTAR SEKARANG"

            // Setup otomatisasi cek NIK (Menunggu input 16 digit pas)
            etNik.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 16) {
                        cekNikOtomatis(s.toString(), etNama, layoutForm)
                    } else {
                        layoutForm.visibility = View.GONE
                        etNama.setText("")
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        btnSimpan.setOnClickListener {
            val nik = etNik.text.toString().trim()
            val nama = etNama.text.toString().trim()
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val dom = etDomisili.text.toString().trim()

            if (isAdminMode) {
                if (nik.length == 16 && nama.isNotEmpty()) {
                    val dataMaster = Pasien("", nama, nik, "", "", "", "")
                    prosesSimpanMaster(dataMaster)
                } else {
                    Toast.makeText(this, "Masukkan 16 digit NIK dan Nama", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (nik.isNotEmpty() && nama.isNotEmpty() && user.isNotEmpty() && pass.isNotEmpty()) {
                    val dataPasien = Pasien("", nama, nik, dom, "", user, pass)
                    prosesSimpan(dataPasien)
                } else {
                    Toast.makeText(this, "Harap lengkapi data. Pastikan NIK terdaftar.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cekNikOtomatis(nik: String, etNama: EditText, layoutForm: LinearLayout) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.cekNik(CekNikRequest(nik))
                if (response.isSuccessful && response.body()?.data != null) {
                    val dataMaster = response.body()?.data
                    etNama.setText(dataMaster?.nama)
                    layoutForm.visibility = View.VISIBLE
                    Toast.makeText(this@RegisterPasienActivity, "Data ditemukan!", Toast.LENGTH_SHORT).show()
                } else {
                    // 🌟 FIX FINAL: Ekstrak string error message dinamis dari Next.js backend
                    var errorMsg = "Silakan hubungi admin klinik untuk pendaftaran NIK" // Fallback default (404)

                    try {
                        val errorBodyString = response.errorBody()?.string()
                        if (!errorBodyString.isNullOrEmpty()) {
                            val errorJson = JSONObject(errorBodyString)
                            // Menarik properti "message" dari payload error JSON server
                            errorMsg = errorJson.optString("message", errorMsg)
                        }
                    } catch (e: Exception) {
                        Log.e("PARSING_ERROR", "Gagal membaca JSON errorBody: ${e.message}")
                    }

                    // Tampilkan pesan dinamis sesungguhnya ke pengguna lewat HP
                    Toast.makeText(this@RegisterPasienActivity, errorMsg, Toast.LENGTH_LONG).show()
                    layoutForm.visibility = View.GONE
                    etNama.setText("")
                }
            } catch (e: Exception) {
                Log.e("CEK_NIK", e.message.toString())
                Toast.makeText(this@RegisterPasienActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prosesSimpan(pasien: Pasien) {
        val btnSimpan = findViewById<Button>(R.id.btnSimpanPasien)
        btnSimpan.isEnabled = false
        btnSimpan.text = "Sedang Mendaftar..."

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.registerPasien(pasien)
                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterPasienActivity, "Berhasil Daftar! Silakan Login", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Data ditolak"
                    Toast.makeText(this@RegisterPasienActivity, "Gagal: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterPasienActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            } finally {
                btnSimpan.isEnabled = true
                btnSimpan.text = "DAFTAR SEKARANG"
            }
        }
    }

    private fun prosesSimpanMaster(pasien: Pasien) {
        val btnSimpan = findViewById<Button>(R.id.btnSimpanPasien)
        btnSimpan.isEnabled = false
        btnSimpan.text = "Menyimpan Master..."

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.addMasterPasien(pasien)
                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterPasienActivity, "Data Master Berhasil Ditambah", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Gagal simpan master"
                    Toast.makeText(this@RegisterPasienActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterPasienActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
            } finally {
                btnSimpan.isEnabled = true
                btnSimpan.text = "SIMPAN DATA MASTER"
            }
        }
    }
}