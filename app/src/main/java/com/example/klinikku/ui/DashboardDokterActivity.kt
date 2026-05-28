package com.example.klinikku.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.model.Pendaftaran
import com.example.klinikku.data.network.RetrofitClient
import com.example.klinikku.databinding.ActivityDashboardDokterBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardDokterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardDokterBinding
    private lateinit var sessionManager: SessionManager

    private lateinit var riwayatAdapter: AntreanRealtimeAdapter
    private lateinit var antreanAdapter: AntreanRealtimeAdapter

    private var countdownTimer: CountDownTimer? = null
    private var activeExamPendaftaran: Pendaftaran? = null
    private var listAntreanAktif: List<Pendaftaran> = emptyList()
    private var totalUlasanCount: Int = 0

    companion object {
        private const val TAG = "DASHBOARD_DOKTER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardDokterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        supportActionBar?.hide()

        binding.ivLogoutKustom.setOnClickListener {
            performLogout()
        }

        setupHeader()
        setupRecyclerViews()
        setupBottomNavigation()
        setupNotificationBell()
        setupBackPressed()
        setupExamActions()
        setupMetricClicks()

        loadDashboardData()
    }
    
    private fun simulasiKirimUlasan(nikDokter: String) {
        val ulasanRef = FirebaseDatabase.getInstance().getReference("ulasan").child(nikDokter)
        val idUlasanBaru = ulasanRef.push().key ?: return
        val dataDummyUlasan = com.example.klinikku.data.model.Ulasan(
            id_antrean = "ANTREAN_XYZ",
            pasien_id = "PASIEN_123",
            nama_pasien = "Pasien Testing",
            bintang = 5.0,
            komentar = "Pelayanan dr. Ammar sangat mantap!"
        )
        ulasanRef.child(idUlasanBaru).setValue(dataDummyUlasan)
        Toast.makeText(this, "Simulasi Ulasan Dikirim!", Toast.LENGTH_SHORT).show()
    }

    private fun setupHeader() {
        val namaDokter = sessionManager.getNama() ?: "Dokter"
        val nikDokter = sessionManager.getNik() ?: "-"
        binding.tvGreeting.text = "Selamat datang, dr. $namaDokter"
        binding.tvNamaDokter.text = namaDokter
        binding.tvNikDokter.text = "NIK: $nikDokter"
    }

    private fun setupRecyclerViews() {
        riwayatAdapter = AntreanRealtimeAdapter(emptyList()) { _ -> }
        binding.rvRiwayatPasien.layoutManager = LinearLayoutManager(this)
        binding.rvRiwayatPasien.adapter = riwayatAdapter

        antreanAdapter = AntreanRealtimeAdapter(emptyList()) { pendaftaran ->
            startPemeriksaan(pendaftaran)
        }
        binding.rvAntreanRealtime.layoutManager = LinearLayoutManager(this)
        binding.rvAntreanRealtime.adapter = antreanAdapter
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_beranda
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_beranda -> {
                    binding.layoutBerandaContent.visibility = View.VISIBLE
                    binding.layoutAntreanContent.visibility = View.GONE
                    true
                }
                R.id.nav_antrean -> {
                    binding.layoutBerandaContent.visibility = View.GONE
                    binding.layoutAntreanContent.visibility = View.VISIBLE
                    loadJadwalBerikutnya()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupNotificationBell() {
        binding.btnNotification.setOnClickListener {
            val count = listAntreanAktif.size
            if (count > 0) {
                Toast.makeText(this, "Anda memiliki $count antrean aktif hari ini!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Belum ada antrean aktif masuk", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showLogoutDialog()
            }
        })
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performLogout() {
        countdownTimer?.cancel()
        sessionManager.logout()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadDashboardData() {
        val nikDokter = sessionManager.getNik() ?: return

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val responsePasien = RetrofitClient.instance.getPasien()
                if (responsePasien.isSuccessful && responsePasien.body() != null) {
                    val listPasien = responsePasien.body()?.data ?: emptyList()
                    binding.tvMetricPasien.text = listPasien.size.toString()
                }

                // Call Ktor jadwal removed to prevent timeout. 
                // Metric Jadwal will be updated by loadJadwalBerikutnya() directly.

                // Ambil ulasan dari Firebase dan hitung rata-rata dengan object Ulasan
                val ulasanRef = FirebaseDatabase.getInstance().getReference("ulasan").child(nikDokter)
                ulasanRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        var totalBintang = 0.0
                        var jumlahUlasan = 0
                        
                        for (data in snapshot.children) {
                            val ulasan = data.getValue(com.example.klinikku.data.model.Ulasan::class.java)
                            if (ulasan != null) {
                                totalBintang += (ulasan.bintang ?: 0.0)
                                jumlahUlasan++
                            }
                        }
                        
                        val rataRata = if (jumlahUlasan > 0) totalBintang / jumlahUlasan else 5.0
                        binding.tvMetricRating.text = String.format(java.util.Locale.getDefault(), "%.1f", rataRata)
                        
                        // Simpan jumlah ulasan dan rata-rata ke AlertDialog listener
                        setupRatingCardClick(rataRata, jumlahUlasan)
                    }
                    
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        binding.tvMetricRating.text = "5.0"
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Error loading metrics: ${e.message}")
            }
        }

        loadRiwayatPasien()
    }

    private fun loadRiwayatPasien() {
        val nikDokter = sessionManager.getNik() ?: return
        val namaDokter = sessionManager.getNama() ?: ""

        val database = FirebaseDatabase.getInstance().getReference("antrean")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val riwayatList = mutableListOf<Pendaftaran>()
                for (data in snapshot.children) {
                    val pendaftaran = data.getValue(Pendaftaran::class.java)
                    if (pendaftaran != null) {
                        val dId = pendaftaran.dokter_id ?: ""
                        val nDok = pendaftaran.nama_dokter ?: ""
                        val poliName = pendaftaran.poli ?: ""
                        val stat = pendaftaran.status ?: ""

                        val matchDokter = (dId == nikDokter) ||
                                (nDok.equals(namaDokter, ignoreCase = true)) ||
                                (poliName.equals("poli kulit kelamin", ignoreCase = true))

                        if (matchDokter && stat.equals("selesai", ignoreCase = true)) {
                            riwayatList.add(pendaftaran)
                        }
                    }
                }
                riwayatList.sortByDescending { it.tanggal_kunjungan ?: "" }
                riwayatAdapter.updateData(riwayatList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error riwayat: ${error.message}")
            }
        })
    }

    private fun setupMetricClicks() {
        binding.btnJadwalPraktek.setOnClickListener {
            val namaDokter = sessionManager.getNama() ?: "Dokter"
            val nikDokter = sessionManager.getNik() ?: "-"
            val shiftAktif = if (listAntreanAktif.isNotEmpty()) listAntreanAktif[0].jam_praktek else "08:00 - 15:00"
            val totalPasien = listAntreanAktif.size
            
            val formatHariIni = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale("id", "ID"))
            val tanggalCantik = formatHariIni.format(java.util.Date())
            
            AlertDialog.Builder(this)
                .setTitle("Rincian Jadwal Praktik")
                .setMessage("Dokter: $namaDokter\nNIK: $nikDokter\n\nHari ini: $tanggalCantik\n• Shift Aktif: $shiftAktif\n• Total Beban Kerja: $totalPasien Antrean Pasien")
                .setPositiveButton("Tutup", null)
                .show()
        }
    }

    private fun setupRatingCardClick(rataRata: Double, jumlahUlasan: Int) {
        val ratingCard = binding.tvMetricRating.parent.parent as? View
        
        // Panggilan simulasi bisa dites via Long Click
        ratingCard?.setOnLongClickListener {
            val nikDokter = sessionManager.getNik() ?: ""
            if (nikDokter.isNotEmpty()) {
                simulasiKirimUlasan(nikDokter)
            }
            true
        }
        
        ratingCard?.setOnClickListener {
            val formattedRating = String.format(java.util.Locale.getDefault(), "%.1f", rataRata)
            AlertDialog.Builder(this)
                .setTitle("Ulasan & Kepuasan Pasien")
                .setMessage("⭐ $formattedRating/5.0\n\nDiakumulasikan dari $jumlahUlasan pasien yang memberikan penilaian.")
                .setPositiveButton("Tutup", null)
                .show()
        }
    }

    private fun loadJadwalBerikutnya() {
        val nikDokter = sessionManager.getNik() ?: return
        val namaDokter = sessionManager.getNama() ?: ""

        val database = FirebaseDatabase.getInstance().getReference("antrean")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawPendaftaranList = mutableListOf<Pendaftaran>()
                for (data in snapshot.children) {
                    val pendaftaran = data.getValue(Pendaftaran::class.java)
                    if (pendaftaran != null) {
                        rawPendaftaranList.add(pendaftaran)
                    }
                }

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val filteredList = rawPendaftaranList.filter { pendaftaran ->
                    val tKunj = pendaftaran.tanggal_kunjungan ?: ""
                    val nDok = pendaftaran.nama_dokter ?: ""
                    val poliName = pendaftaran.poli ?: ""
                    val dId = pendaftaran.dokter_id ?: ""
                    val stat = pendaftaran.status ?: ""

                    val matchTanggal = tKunj == todayStr
                    val matchDokter = (nDok.equals(namaDokter, ignoreCase = true)) ||
                            (poliName.equals("poli kulit kelamin", ignoreCase = true)) ||
                            (dId == nikDokter)
                    val matchStatus = !stat.equals("selesai", ignoreCase = true)

                    matchTanggal && matchDokter && matchStatus
                }.sortedBy { it.jam_praktek ?: "" }

                listAntreanAktif = filteredList
                
                // Hitung metrik jadwal langsung dari list antrean aktif (realtime efisien)
                binding.tvMetricJadwal.text = filteredList.size.toString()

                if (filteredList.isNotEmpty()) {
                    binding.rvAntreanRealtime.visibility = View.VISIBLE
                    binding.layoutAntreanKosong.visibility = View.GONE
                    antreanAdapter.updateData(filteredList)
                } else {
                    binding.rvAntreanRealtime.visibility = View.GONE
                    binding.layoutAntreanKosong.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error antrean: ${error.message}")
                binding.rvAntreanRealtime.visibility = View.GONE
                binding.layoutAntreanKosong.visibility = View.VISIBLE
            }
        })
    }

    private fun startPemeriksaan(pendaftaran: Pendaftaran) {
        val status = pendaftaran.status ?: ""
        if (status.equals("selesai", ignoreCase = true)) {
            Toast.makeText(this, "Pasien ini sudah selesai diperiksa", Toast.LENGTH_SHORT).show()
            return
        }

        val idAntrean = pendaftaran.id_antrean
        if (!idAntrean.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().getReference("antrean")
                .child(idAntrean).child("status").setValue("sedang diproses")
        }

        countdownTimer?.cancel()
        activeExamPendaftaran = pendaftaran

        binding.tvNamaPasienAktif.text = pendaftaran.nama_pasien ?: "Pasien"
        binding.cardPemeriksaanAktif.visibility = View.VISIBLE

        countdownTimer = object : CountDownTimer(1800000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvWaktuPemeriksaan.text = String.format(Locale.getDefault(), "Sisa waktu: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.tvWaktuPemeriksaan.text = "Waktu Pemeriksaan Selesai!"
            }
        }.start()

        Toast.makeText(this, "Memulai pemeriksaan untuk ${pendaftaran.nama_pasien}", Toast.LENGTH_SHORT).show()
        
        val keluhan = pendaftaran.keluhan
        if (!keluhan.isNullOrBlank()) {
            Toast.makeText(this, "Keluhan: $keluhan", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupExamActions() {
        binding.btnSelesaiPeriksa.setOnClickListener {
            // Tampilkan form diagnosa & obat sebelum menyelesaikan pemeriksaan
            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }
            
            val etDiagnosa = android.widget.EditText(this).apply {
                hint = "Diagnosa Pasien"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                maxLines = 3
            }
            
            val etObat = android.widget.EditText(this).apply {
                hint = "Resep Obat"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                maxLines = 3
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 20
                }
            }
            
            container.addView(etDiagnosa)
            container.addView(etObat)
            
            AlertDialog.Builder(this)
                .setTitle("Selesaikan Pemeriksaan")
                .setMessage("Silakan isi hasil diagnosa dan resep obat untuk pasien:")
                .setView(container)
                .setPositiveButton("Simpan & Selesai") { _, _ ->
                    val diagnosa = etDiagnosa.text.toString().trim()
                    val obat = etObat.text.toString().trim()
                    
                    if (diagnosa.isEmpty() || obat.isEmpty()) {
                        Toast.makeText(this, "Diagnosa dan Obat wajib diisi!", Toast.LENGTH_SHORT).show()
                    } else {
                        finishActiveExamWithData("Data rekam medis pasien berhasil disimpan!", diagnosa, obat)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnNextAntrean.setOnClickListener {
            val currentActive = activeExamPendaftaran
            if (currentActive != null) {
                val currentIndex = listAntreanAktif.indexOfFirst { it.pasien_id == currentActive.pasien_id }
                if (currentIndex != -1 && currentIndex + 1 < listAntreanAktif.size) {
                    val nextPatient = listAntreanAktif[currentIndex + 1]
                    finishActiveExam("Berpindah ke antrean berikutnya.")
                    startPemeriksaan(nextPatient)
                } else {
                    finishActiveExam("Semua antrean hari ini telah selesai diperiksa.")
                }
            } else {
                Toast.makeText(this, "Tidak ada pemeriksaan aktif.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun finishActiveExamWithData(message: String, diagnosa: String, obat: String) {
        countdownTimer?.cancel()

        val idAntrean = activeExamPendaftaran?.id_antrean
        if (!idAntrean.isNullOrEmpty()) {
            val updateMap = mapOf(
                "status" to "selesai",
                "diagnosa" to diagnosa,
                "obat" to obat
            )
            FirebaseDatabase.getInstance().getReference("antrean")
                .child(idAntrean).updateChildren(updateMap)
        }

        binding.cardPemeriksaanAktif.visibility = View.GONE
        activeExamPendaftaran = null
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun finishActiveExam(message: String) {
        countdownTimer?.cancel()

        val idAntrean = activeExamPendaftaran?.id_antrean
        if (!idAntrean.isNullOrEmpty()) {
            FirebaseDatabase.getInstance().getReference("antrean")
                .child(idAntrean).child("status").setValue("selesai")
        }

        binding.cardPemeriksaanAktif.visibility = View.GONE
        activeExamPendaftaran = null
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }
}