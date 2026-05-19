package com.example.klinikku.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.model.Dokter
import com.example.klinikku.data.network.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adapterPasien: PasienAdapter
    private lateinit var adapterDokter: DokterAdapter
    private lateinit var adapterPoli: PoliAdapter
    
    private lateinit var layoutPasien: LinearLayout
    private lateinit var layoutDokter: LinearLayout
    private lateinit var layoutPoli: LinearLayout
    
    private lateinit var menuPasien: LinearLayout
    private lateinit var menuDokter: LinearLayout
    private lateinit var menuPoli: LinearLayout
    
    private lateinit var ivPasien: ImageView
    private lateinit var tvPasien: TextView
    private lateinit var ivDokter: ImageView
    private lateinit var tvDokter: TextView
    private lateinit var ivPoli: ImageView
    private lateinit var tvPoli: TextView

    private lateinit var fabPasien: FloatingActionButton
    private lateinit var fabDokter: FloatingActionButton
    private lateinit var fabPoli: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi UI
        layoutPasien = findViewById(R.id.layoutContainerPasien)
        layoutDokter = findViewById(R.id.layoutContainerDokter)
        layoutPoli = findViewById(R.id.layoutContainerPoli)
        
        menuPasien = findViewById(R.id.menuPasien)
        menuDokter = findViewById(R.id.menuDokter)
        menuPoli = findViewById(R.id.menuPoli)
        
        ivPasien = findViewById(R.id.ivMenuPasien)
        tvPasien = findViewById(R.id.tvMenuPasien)
        ivDokter = findViewById(R.id.ivMenuDokter)
        tvDokter = findViewById(R.id.tvMenuDokter)
        ivPoli = findViewById(R.id.ivMenuPoli)
        tvPoli = findViewById(R.id.tvMenuPoli)

        fabPasien = findViewById(R.id.fabAddPasien)
        fabDokter = findViewById(R.id.fabAddDokter)
        fabPoli = findViewById(R.id.fabAddPoli)

        // Bind & set up unified logout
        val sessionManager = SessionManager(this)
        val btnLogout = findViewById<ImageView>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // 2. Setup RecyclerViews
        setupRecyclerViews()

        // 3. Logika Navigasi Footer
        menuPasien.setOnClickListener { switchTab("PASIEN") }
        menuDokter.setOnClickListener { switchTab("DOKTER") }
        menuPoli.setOnClickListener { switchTab("POLI") }

        // 4. Logika FAB
        fabPasien.setOnClickListener {
            val intent = Intent(this, RegisterPasienActivity::class.java)
            intent.putExtra("is_admin_mode", true)
            startActivity(intent)
        }

        fabDokter.setOnClickListener {
            startActivity(Intent(this, TambahDokterActivity::class.java))
        }

        fabPoli.setOnClickListener {
            startActivity(Intent(this, TambahPoliActivity::class.java))
        }

        // 5. Muat Data Awal
        switchTab("PASIEN")
    }

    private fun setupRecyclerViews() {
        // Pasien
        val rvPasien = findViewById<RecyclerView>(R.id.rvPasien)
        rvPasien.layoutManager = LinearLayoutManager(this)
        adapterPasien = PasienAdapter(listOf())
        rvPasien.adapter = adapterPasien

        // Dokter
        val rvDokter = findViewById<RecyclerView>(R.id.rvDokter)
        rvDokter.layoutManager = LinearLayoutManager(this)
        adapterDokter = DokterAdapter(
            listDokter = listOf(),
            onEditClick = { dokter -> launchEditDokter(dokter) },
            onDeleteClick = { dokter -> konfirmasiHapusDokter(dokter) }
        )
        rvDokter.adapter = adapterDokter

        // Poli
        val rvPoli = findViewById<RecyclerView>(R.id.rvPoli)
        rvPoli.layoutManager = LinearLayoutManager(this)
        adapterPoli = PoliAdapter(listOf())
        rvPoli.adapter = adapterPoli
    }

    private fun switchTab(tab: String) {
        // Hide all containers and FABs
        layoutPasien.visibility = View.GONE
        layoutDokter.visibility = View.GONE
        layoutPoli.visibility = View.GONE
        fabPasien.visibility = View.GONE
        fabDokter.visibility = View.GONE
        fabPoli.visibility = View.GONE

        // Reset all menu colors
        val inactiveColor = getColor(android.R.color.darker_gray)
        ivPasien.setColorFilter(inactiveColor); tvPasien.setTextColor(inactiveColor)
        ivDokter.setColorFilter(inactiveColor); tvDokter.setTextColor(inactiveColor)
        ivPoli.setColorFilter(inactiveColor); tvPoli.setTextColor(inactiveColor)

        val activeColor = getColor(R.color.clinic_blue)

        when (tab) {
            "PASIEN" -> {
                layoutPasien.visibility = View.VISIBLE
                fabPasien.visibility = View.VISIBLE
                ivPasien.setColorFilter(activeColor)
                tvPasien.setTextColor(activeColor)
                muatDataPasien()
            }
            "DOKTER" -> {
                layoutDokter.visibility = View.VISIBLE
                fabDokter.visibility = View.VISIBLE
                ivDokter.setColorFilter(activeColor)
                tvDokter.setTextColor(activeColor)
                muatDataDokter()
            }
            "POLI" -> {
                layoutPoli.visibility = View.VISIBLE
                fabPoli.visibility = View.VISIBLE
                ivPoli.setColorFilter(activeColor)
                tvPoli.setTextColor(activeColor)
                muatDataPoli()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data yang sedang aktif
        if (layoutPasien.visibility == View.VISIBLE) muatDataPasien()
        if (layoutDokter.visibility == View.VISIBLE) muatDataDokter()
        if (layoutPoli.visibility == View.VISIBLE) muatDataPoli()
    }

    private fun muatDataPasien() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPasien()
                if (response.isSuccessful) {
                    val listData = response.body()?.data ?: listOf()
                    adapterPasien.updateData(listData)
                }
            } catch (e: Exception) {
                Log.e("MAIN_ACTIVITY", "Error Pasien: ${e.message}")
            }
        }
    }

    private fun muatDataDokter() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getDokter()
                if (response.isSuccessful) {
                    val listData = response.body()?.data ?: listOf()
                    adapterDokter.updateData(listData)
                }
            } catch (e: Exception) {
                Log.e("MAIN_ACTIVITY", "Error Dokter: ${e.message}")
            }
        }
    }

    private fun muatDataPoli() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getPoli()
                if (response.isSuccessful) {
                    val listData = response.body()?.data ?: listOf()
                    adapterPoli.updateData(listData)
                }
            } catch (e: Exception) {
                Log.e("MAIN_ACTIVITY", "Error Poli: ${e.message}")
            }
        }
    }

    // ===== DOKTER: Edit Mode =====
    private fun launchEditDokter(dokter: Dokter) {
        val intent = Intent(this, TambahDokterActivity::class.java).apply {
            putExtra("EDIT_MODE_DOKTER", true)
            putExtra("DOKTER_NIK", dokter.nik)
            putExtra("DOKTER_NAMA", dokter.nama)
            putExtra("DOKTER_POLI", dokter.poli)
            putExtra("DOKTER_SPESIALIS", dokter.spesialis)
            putExtra("DOKTER_USERNAME", dokter.username)
            putExtra("DOKTER_PASSWORD", dokter.password)
            putExtra("DOKTER_HARI", dokter.hari)
            putExtra("DOKTER_JAM_MULAI", dokter.jam_mulai)
            putExtra("DOKTER_JAM_SELESAI", dokter.jam_selesai)
            putExtra("DOKTER_ID_JADWAL", dokter.id_jadwal)
        }
        startActivity(intent)
    }

    // ===== DOKTER: Delete Confirmation =====
    private fun konfirmasiHapusDokter(dokter: Dokter) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Dokter")
            .setMessage("Apakah Anda yakin ingin menghapus dokter ini?\n\n${dokter.nama} (${dokter.poli})")
            .setPositiveButton("Hapus") { _, _ -> eksekusiHapusDokter(dokter) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun eksekusiHapusDokter(dokter: Dokter) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.deleteDokter(
                    nik = dokter.nik,
                    idJadwal = dokter.id_jadwal
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Dokter berhasil dihapus", Toast.LENGTH_SHORT).show()
                    muatDataDokter() // Refresh list dynamically
                } else {
                    val errMsg = response.errorBody()?.string() ?: "Gagal menghapus dokter"
                    Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                    Log.e("HAPUS_DOKTER", errMsg)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("HAPUS_DOKTER", "Exception: ${e.message}")
            }
        }
    }
}
