package com.example.klinikku.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.example.klinikku.data.model.CancelRequest
import com.example.klinikku.data.model.Pendaftaran
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RiwayatAntreanActivity : AppCompatActivity() {

    private lateinit var rvRiwayat: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageView
    
    private var loggedInNik: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riwayat_antrean)

        rvRiwayat = findViewById(R.id.rvRiwayatAntrean)
        tvEmptyState = findViewById(R.id.tvEmptyStateRiwayat)
        progressBar = findViewById(R.id.progressBarRiwayat)
        btnBack = findViewById(R.id.btnBackRiwayat)

        btnBack.setOnClickListener {
            finish()
        }

        rvRiwayat.layoutManager = LinearLayoutManager(this)

        val sessionManager = SessionManager(this)
        loggedInNik = sessionManager.getNik()

        if (!loggedInNik.isNullOrEmpty()) {
            loadRiwayat()
        } else {
            Toast.makeText(this, "NIK tidak ditemukan, silakan login ulang.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadRiwayat() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        rvRiwayat.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getRiwayatPendaftaran(loggedInNik!!)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val historyList = response.body()!!
                        
                        // Sort so newest dates or pending appear first
                        val sortedList = historyList.sortedByDescending { it.tanggal_kunjungan }

                        if (sortedList.isEmpty()) {
                            tvEmptyState.visibility = View.VISIBLE
                        } else {
                            rvRiwayat.visibility = View.VISIBLE
                            val adapter = RiwayatAntreanAdapter(sortedList) { idAntrean ->
                                showCancelConfirmation(idAntrean)
                            }
                            rvRiwayat.adapter = adapter
                        }
                    } else {
                        tvEmptyState.visibility = View.VISIBLE
                        tvEmptyState.text = "Gagal memuat riwayat."
                        Toast.makeText(this@RiwayatAntreanActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Terjadi kesalahan koneksi."
                    Toast.makeText(this@RiwayatAntreanActivity, "Kesalahan jaringan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCancelConfirmation(antreanId: String) {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Antrean")
            .setMessage("Apakah Anda yakin ingin membatalkan antrean ini?")
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                cancelAntrean(antreanId)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun cancelAntrean(antreanId: String) {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.cancelAntrean(CancelRequest(antreanId))
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.status == "success") {
                            Toast.makeText(this@RiwayatAntreanActivity, "Antrean berhasil dibatalkan.", Toast.LENGTH_SHORT).show()
                            loadRiwayat() // Refresh list
                        } else {
                            Toast.makeText(this@RiwayatAntreanActivity, body.message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Attempt to extract error message if 400 Bad Request
                        val errorString = response.errorBody()?.string()
                        Toast.makeText(this@RiwayatAntreanActivity, "Gagal: $errorString", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@RiwayatAntreanActivity, "Kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
