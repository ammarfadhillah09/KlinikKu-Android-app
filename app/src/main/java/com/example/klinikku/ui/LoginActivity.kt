package com.example.klinikku.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.model.LoginRequest
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        btnLogin.setOnClickListener {
            val user = etUsername.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                // 1. Matikan tombol sementara agar user tidak double-click (mencegah crash)
                btnLogin.isEnabled = false
                btnLogin.text = "Loading..."

                prosesLogin(user, pass, btnLogin)
            } else {
                Toast.makeText(this, "Harap isi username dan password", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterPasienActivity::class.java))
        }
    }

    private fun prosesLogin(user: String, pass: String, btnLogin: Button) {
        // 2. Jalankan Coroutine secara eksplisit di Main Thread untuk keamanan UI
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val request = LoginRequest(user, pass)
                val response = RetrofitClient.instance.loginUser(request)

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()
                    val loginData = loginResponse?.data

                    if (loginData != null) {
                        val role = loginData.role
                        val nik = loginData.nik
                        val nama = loginData.nama

                        Log.d("DEBUG_KLINIK", "Login Berhasil: Role=$role, NIK=$nik")

                        // 3. Simpan Session ke SharedPreferences
                        val sessionManager = SessionManager(this@LoginActivity)
                        sessionManager.saveSession(nik ?: "", nama ?: "", role ?: "")

                        Toast.makeText(this@LoginActivity, "Selamat Datang, $nama!", Toast.LENGTH_SHORT).show()

                        // 4. Navigasi Berdasarkan Role (admin / dokter / pasien)
                        when (role) {
                            "admin" -> {
                                // Admin masuk ke Dashboard Admin (MainActivity)
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                            "dokter" -> {
                                // Dokter masuk ke Dashboard Dokter
                                val intent = Intent(this@LoginActivity, DashboardDokterActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            else -> {
                                // Default: Pasien masuk ke Detail Pasien
                                if (!nik.isNullOrEmpty()) {
                                    val intent = Intent(this@LoginActivity, DetailPasienActivity::class.java)
                                    intent.putExtra("NIK_PASIEN", nik)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // JIKA NIK KOSONG, JANGAN ADA FINISH()! BIARKAN USER TETAP DI HALAMAN LOGIN
                                    Toast.makeText(this@LoginActivity, "Data NIK kosong di server!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Data user tidak valid", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login Gagal: Username/Password salah", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 5. Tangkap semua Error (Internet mati, Tunnel mati, JSON error) tanpa Force Close
                Log.e("LOGIN_ERROR", "Crash berhasil dicegah! Pesan: ${e.message}", e)
                Toast.makeText(this@LoginActivity, "Koneksi Error: Cek Server/Tunnel", Toast.LENGTH_LONG).show()
            } finally {
                // 6. Apapun yang terjadi (sukses/gagal), nyalakan lagi tombolnya
                btnLogin.isEnabled = true
                btnLogin.text = "Login"
            }
        }
    }
}