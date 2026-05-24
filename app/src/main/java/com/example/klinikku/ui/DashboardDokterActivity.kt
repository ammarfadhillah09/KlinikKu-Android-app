package com.example.klinikku.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.network.RetrofitClient
import com.example.klinikku.databinding.ActivityDashboardDokterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * DashboardDokterActivity
 *
 * Halaman utama setelah aktor Dokter berhasil login.
 * Menampilkan profil dokter (Nama & NIK) yang diambil dari SharedPreferences
 * dan menyediakan kerangka Coroutine untuk operasi data di masa depan.
 */
class DashboardDokterActivity : AppCompatActivity() {

    // View Binding — di-inflate dari activity_dashboard_dokter.xml
    private lateinit var binding: ActivityDashboardDokterBinding

    // SessionManager untuk akses SharedPreferences (baca profil & logout)
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Paksa mode terang agar konsisten dengan halaman lain
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // 1. Inflate View Binding
        binding = ActivityDashboardDokterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Inisialisasi SessionManager
        sessionManager = SessionManager(this)

        // 3. Ambil data Dokter dari SharedPreferences yang sudah disimpan saat login
        val namaDokter = sessionManager.getNama() ?: "Dokter"
        val nikDokter = sessionManager.getNik() ?: "-"

        // 4. Tampilkan data ke TextView via View Binding
        binding.tvNamaDokter.text = namaDokter
        binding.tvNikDokter.text = nikDokter

        // 5. Setup Menu Klik — placeholder untuk fitur masa depan
        binding.btnMenuJadwalPraktek.setOnClickListener {
            Toast.makeText(this, "Fitur Jadwal Praktek akan segera hadir", Toast.LENGTH_SHORT).show()
        }

        binding.btnMenuAntreanPasien.setOnClickListener {
            Toast.makeText(this, "Fitur Daftar Antrean Pasien akan segera hadir", Toast.LENGTH_SHORT).show()
        }

        // 6. Tombol Logout — hapus sesi lalu kembali ke LoginActivity
        binding.btnMenuLogout.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                // Bersihkan seluruh back stack agar tidak bisa kembali tanpa login
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    // ===========================================================================================
    // BOILERPLATE COROUTINE — Gunakan fungsi ini sebagai template untuk operasi data Dokter
    // ===========================================================================================

    /**
     * Template fungsi Coroutine yang aman untuk pemanggilan API.
     * Menangani IOException (jaringan mati) dan HttpException (error server)
     * secara terpisah agar pesan error lebih informatif ke pengguna.
     *
     * Cara pakai:
     *   Ganti isi blok `// TODO:` dengan pemanggilan RetrofitClient.instance.namaEndpoint()
     */
    private fun contohOperasiData() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // TODO: Panggil API di sini, contoh:
                // val response = RetrofitClient.instance.getDokter()
                // if (response.isSuccessful) { ... }

                Log.d("DASHBOARD_DOKTER", "Operasi data berhasil dijalankan")

            } catch (e: IOException) {
                // Kasus: Tidak ada koneksi internet atau server tidak bisa dijangkau
                Log.e("DASHBOARD_DOKTER", "IOException: ${e.message}", e)
                Toast.makeText(
                    this@DashboardDokterActivity,
                    "Koneksi Error: Periksa internet atau server",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: HttpException) {
                // Kasus: Server merespon dengan HTTP error code (4xx / 5xx)
                Log.e("DASHBOARD_DOKTER", "HttpException: ${e.code()} - ${e.message()}", e)
                Toast.makeText(
                    this@DashboardDokterActivity,
                    "Server Error: ${e.code()} ${e.message()}",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                // Kasus: Error tak terduga lainnya (JSON parsing, dll.)
                Log.e("DASHBOARD_DOKTER", "Exception: ${e.message}", e)
                Toast.makeText(
                    this@DashboardDokterActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
