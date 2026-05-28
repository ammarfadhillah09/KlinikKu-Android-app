package com.example.klinikku.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klinikku.data.network.RetrofitClient
import com.example.klinikku.databinding.ActivityDaftarAntreanDokBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * DaftarAntreanDokterActivity
 *
 * Menampilkan daftar pasien antrean hari ini dalam bentuk RecyclerView.
 * Data diambil secara mandiri (fetch ulang) dari endpoint getPasien()
 * agar selalu menampilkan data terbaru dari server.
 */
class DaftarAntreanDokterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarAntreanDokBinding
    private lateinit var adapterAntrean: AntreanDokterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // 1. Inflate View Binding
        binding = ActivityDaftarAntreanDokBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Setup RecyclerView dengan LayoutManager dan Adapter kosong
        adapterAntrean = AntreanDokterAdapter(emptyList()) { pendaftaran ->
            // Do nothing on click for now, or handle click if needed
        }
        binding.rvAntreanPasien.layoutManager = LinearLayoutManager(this)
        binding.rvAntreanPasien.adapter = adapterAntrean

        // 3. Tombol Kembali ke Dashboard Dokter
        binding.btnKembali.setOnClickListener {
            finish()
        }

        // 4. Muat data pasien dari server saat halaman dibuka
        muatDataAntrean()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data setiap kali kembali ke halaman ini
        muatDataAntrean()
    }

    /**
     * muatDataAntrean
     *
     * Fetch daftar pasien dari Vercel Cloud via getPasien().
     * Jika berhasil, isi RecyclerView. Jika kosong, tampilkan empty state.
     * Error handling bertingkat: IOException → HttpException → Exception.
     */
    private fun muatDataAntrean() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.getPasien()

                if (response.isSuccessful && response.body() != null) {
                    val listPasien = response.body()?.data ?: emptyList()

                    if (listPasien.isNotEmpty()) {
                        // Data ada — tampilkan RecyclerView, sembunyikan empty state
                        binding.rvAntreanPasien.visibility = View.VISIBLE
                        binding.tvEmptyState.visibility = View.GONE

                        // Map data Pasien to Pendaftaran for the adapter
                        val listPendaftaran = listPasien.mapIndexed { index, pasien ->
                            com.example.klinikku.data.model.Pendaftaran(
                                id_antrean = "antrean_${pasien.nik}",
                                pasien_id = pasien.nik,
                                nama_pasien = pasien.nama,
                                dokter_id = "",
                                nama_dokter = "",
                                poli = "Umum",
                                tanggal_kunjungan = pasien.tanggal_daftar,
                                jam_praktek = "Menunggu",
                                nomor_antrean = index + 1,
                                status = "Menunggu"
                            )
                        }

                        // Update adapter dengan data baru
                        adapterAntrean.updateData(listPendaftaran)

                        // Update badge jumlah pasien di header
                        binding.tvJumlahPasien.text = "${listPasien.size} pasien"

                        Log.d("ANTREAN_DOKTER", "Berhasil memuat ${listPasien.size} pasien antrean")
                    } else {
                        // Data kosong — sembunyikan RecyclerView, tampilkan empty state
                        binding.rvAntreanPasien.visibility = View.GONE
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.tvJumlahPasien.text = "0 pasien"

                        Log.d("ANTREAN_DOKTER", "Data pasien antrean kosong")
                    }
                } else {
                    // Response gagal — tampilkan pesan error dari server
                    val errorMsg = response.errorBody()?.string() ?: "Gagal memuat data antrean"
                    Log.e("ANTREAN_DOKTER", "Response gagal: ${response.code()} - $errorMsg")
                    Toast.makeText(this@DaftarAntreanDokterActivity, "Gagal: $errorMsg", Toast.LENGTH_LONG).show()

                    binding.rvAntreanPasien.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }

            } catch (e: IOException) {
                // Jaringan mati atau server tidak bisa dijangkau
                Log.e("ANTREAN_DOKTER", "IOException: ${e.message}", e)
                Toast.makeText(
                    this@DaftarAntreanDokterActivity,
                    "Koneksi Error: Periksa internet atau server",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: HttpException) {
                // Server mengembalikan HTTP error code (4xx/5xx)
                Log.e("ANTREAN_DOKTER", "HttpException: ${e.code()} - ${e.message()}", e)
                Toast.makeText(
                    this@DaftarAntreanDokterActivity,
                    "Server Error: ${e.code()} ${e.message()}",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                // Error tak terduga (JSON parsing error, dll.)
                Log.e("ANTREAN_DOKTER", "Exception: ${e.message}", e)
                Toast.makeText(
                    this@DaftarAntreanDokterActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
