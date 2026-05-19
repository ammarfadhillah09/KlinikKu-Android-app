package com.example.klinikku.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailPasienActivity : AppCompatActivity() {

    private var nikPasien: String? = null
    private var loggedInNik: String? = null

    // Views untuk tiket antrean aktif
    private lateinit var cardAntreanAktif: CardView
    private lateinit var tvNomorAntrean: TextView
    private lateinit var tvPoliTujuan: TextView
    private lateinit var tvNamaDokter: TextView
    private lateinit var tvJamPraktek: TextView
    private lateinit var tvAntreanToken: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_pasien)

        val tvNama = findViewById<TextView>(R.id.tvDetailNama)
        val tvNik = findViewById<TextView>(R.id.tvDetailNik)
        val tvDomisili = findViewById<TextView>(R.id.tvDetailDomisili)
        val btnHapus = findViewById<Button>(R.id.btnHapusPasien)
        
        // Menu Dashboard Elements
        val tvMenuTitle = findViewById<TextView>(R.id.tvMenuTitle)
        val layoutMenu = findViewById<View>(R.id.layoutMenuDashboard)
        val btnMenuProfilMedis = findViewById<View>(R.id.btnMenuProfilMedis)
        val btnMenuDaftarLayanan = findViewById<View>(R.id.btnMenuDaftarLayanan)

        // Inisialisasi Views Antrean Aktif
        cardAntreanAktif = findViewById(R.id.cardAntreanAktif)
        tvNomorAntrean  = findViewById(R.id.tvNomorAntrean)
        tvPoliTujuan    = findViewById(R.id.tvPoliTujuan)
        tvNamaDokter    = findViewById(R.id.tvNamaDokter)
        tvJamPraktek    = findViewById(R.id.tvJamPraktek)
        tvAntreanToken  = findViewById(R.id.tvAntreanToken)

        // 1. Cek Role untuk Tombol & Menu
        val sessionManager = SessionManager(this)
        val roleUser = sessionManager.getRole() ?: "pasien"
        loggedInNik = sessionManager.getNik()

        // Aturan: Admin bisa hapus, Pasien bisa lihat Menu Dashboard & Antrean Aktif
        btnHapus.visibility = if (roleUser == "admin") View.VISIBLE else View.GONE

        val isPasien = roleUser == "pasien"
        tvMenuTitle.visibility = if (isPasien) View.VISIBLE else View.GONE
        layoutMenu.visibility = if (isPasien) View.VISIBLE else View.GONE

        // Fetch antrean aktif hanya untuk role pasien
        if (isPasien && !loggedInNik.isNullOrEmpty()) {
            muatAntreanAktif()
        }

        // 2. Tangkap NIK dari Intent
        nikPasien = intent.getStringExtra("NIK_PASIEN")

        if (!nikPasien.isNullOrEmpty()) {
            muatDetailPasien(nikPasien!!, tvNama, tvNik, tvDomisili)
        } else {
            tvNama.text = "Data Tidak Ditemukan"
            Toast.makeText(this, "NIK tidak valid", Toast.LENGTH_SHORT).show()
        }

        // 3. Logika Klik Menu Dashboard
        btnMenuProfilMedis.setOnClickListener {
            startActivity(Intent(this, ProfilMedisActivity::class.java))
        }

        // 4. Klik Menu Daftar Layanan
        btnMenuDaftarLayanan.setOnClickListener {
            // Membuka halaman Booking Antrean (Sprint Booking Antrean)
            startActivity(Intent(this, BookingAntreanActivity::class.java))
        }

        // 4.5 Klik Menu Riwayat Antrean
        val btnMenuRiwayatAntrean = findViewById<View>(R.id.btnMenuRiwayatAntrean)
        btnMenuRiwayatAntrean?.setOnClickListener {
            startActivity(Intent(this, RiwayatAntreanActivity::class.java))
        }

        // 4.6 Klik Menu Logout
        val btnMenuLogout = findViewById<View>(R.id.btnMenuLogout)
        btnMenuLogout?.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // 5. Logika Hapus Pasien (Khusus Admin)
        btnHapus.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hapus Data")
            builder.setMessage("Apakah Anda yakin ingin menghapus pasien ini?")
            builder.setPositiveButton("Ya") { _, _ ->
                nikPasien?.let { prosesHapusPasien(it) }
            }
            builder.setNegativeButton("Batal", null)
            builder.show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh tiket antrean setiap kali pasien kembali ke layar ini
        val sessionManager = SessionManager(this)
        val role = sessionManager.getRole() ?: "pasien"
        if (role == "pasien") {
            muatAntreanAktif()
        }
    }

    /**
     * muatAntreanAktif
     *
     * Fetches the logged-in patient's active queue from the API.
     * Binds the details to the live status card:
     *   - Queue Number to tvNomorAntrean
     *   - Poli to tvPoliTujuan
     *   - Doctor name to tvNamaDokter
     *   - Schedule hours to tvJamPraktek
     *   - Token to tvAntreanToken
     *
     * Performs a secondary fetch to map id_jadwal -> Doctor Name & Jam Praktek
     * for real-time consistency.
     */
    private fun muatAntreanAktif() {
        val sessionManager = SessionManager(this)
        val nik = sessionManager.getNik() ?: return

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.getAntreanAktif(nik)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    when {
                        // CASE A: Antrean aktif ditemukan — tampilkan tiket
                        body.status == "success" && body.data != null -> {
                            val antrean = body.data
                            tvNomorAntrean.text = antrean.nomor_antrean.toString()
                            tvPoliTujuan.text   = antrean.poli ?: "Umum"
                            tvAntreanToken.text = antrean.token?.ifBlank { "-" } ?: "-"
                            cardAntreanAktif.visibility = View.VISIBLE

                            // Fetch doctor details to resolve Doctor Name & Jam Praktek
                            val idJadwal = antrean.id_jadwal
                            if (!idJadwal.isNullOrBlank()) {
                                try {
                                    val dokterRes = RetrofitClient.instance.getDokter()
                                    if (dokterRes.isSuccessful && dokterRes.body() != null) {
                                        val dokterList = dokterRes.body()!!.data ?: emptyList()
                                        val matchingDokter = dokterList.find { it.id_jadwal == idJadwal }
                                        if (matchingDokter != null) {
                                            tvNamaDokter.text = matchingDokter.nama
                                            tvJamPraktek.text = "${matchingDokter.jam_mulai} - ${matchingDokter.jam_selesai}"
                                        } else {
                                            // Fallback
                                            tvNamaDokter.text = antrean.nama_dokter ?: "-"
                                            tvJamPraktek.text = "-"
                                        }
                                    } else {
                                        tvNamaDokter.text = antrean.nama_dokter ?: "-"
                                        tvJamPraktek.text = "-"
                                    }
                                } catch (ex: Exception) {
                                    Log.e("DETAIL_PASIEN", "Gagal fetch detail dokter: ${ex.message}")
                                    tvNamaDokter.text = antrean.nama_dokter ?: "-"
                                    tvJamPraktek.text = "-"
                                }
                            } else {
                                tvNamaDokter.text = antrean.nama_dokter ?: "-"
                                tvJamPraktek.text = "-"
                            }
                        }

                        // CASE B: Jam praktek berakhir — antrean dipindahkan ke riwayat
                        body.status == "expired" -> {
                            cardAntreanAktif.visibility = View.GONE
                            Toast.makeText(
                                this@DetailPasienActivity,
                                "Jam praktek berakhir, antrean Anda dipindahkan ke riwayat",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // CASE C: Tidak ada antrean aktif
                        else -> {
                            cardAntreanAktif.visibility = View.GONE
                        }
                    }
                } else {
                    cardAntreanAktif.visibility = View.GONE
                    Log.w("DETAIL_PASIEN", "getAntreanAktif gagal: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                cardAntreanAktif.visibility = View.GONE
                Log.e("DETAIL_PASIEN", "Error getAntreanAktif: ${e.message}", e)
            }
        }
    }

    private fun muatDetailPasien(nik: String, tvNama: TextView, tvNik: TextView, tvDom: TextView) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getDetailPasien(nik)

                if (response.isSuccessful && response.body()?.data != null) {
                    val data = response.body()?.data
                    tvNama.text = data?.nama
                    tvNik.text = data?.nik
                    tvDom.text = data?.domisili
                } else {
                    tvNama.text = "Pasien tidak ditemukan"
                    Toast.makeText(this@DetailPasienActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                tvNama.text = "Kesalahan Koneksi"
                Toast.makeText(this@DetailPasienActivity, "Cek koneksi internet/tunnel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prosesHapusPasien(nik: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deletePasien(nik)
                if (response.isSuccessful) {
                    Toast.makeText(this@DetailPasienActivity, "Data Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                    finish() 
                } else {
                    Toast.makeText(this@DetailPasienActivity, "Gagal menghapus dari server", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DetailPasienActivity, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
