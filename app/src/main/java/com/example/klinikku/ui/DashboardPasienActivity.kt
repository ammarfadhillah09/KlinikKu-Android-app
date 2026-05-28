package com.example.klinikku.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.klinikku.data.local.SessionManager
import com.example.klinikku.data.model.Pendaftaran
import com.example.klinikku.data.model.Ulasan
import com.example.klinikku.databinding.ActivityDashboardPasienBinding
import com.example.klinikku.databinding.DialogRatingPasienBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardPasienActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardPasienBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapterAntrean: AntreanRealtimeAdapter
    private var listAntreanPasien: List<Pendaftaran> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        
        binding = ActivityDashboardPasienBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        
        setupHeader()
        setupMenuGrid()
        setupRecyclerView()
        loadAntreanRealtime()
    }

    private fun setupHeader() {
        val namaPasien = sessionManager.getNama() ?: "Pasien"
        val nikPasien = sessionManager.getNik() ?: "-"
        
        binding.tvGreetingPasien.text = "Halo, $namaPasien"
        binding.tvNikPasien.text = "NIK: $nikPasien"
        
        val formatHariIni = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        binding.tvTanggalHariIni.text = formatHariIni.format(Date())
        
        binding.btnLogout.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupMenuGrid() {
        binding.btnDaftarAntrean.setOnClickListener {
            startActivity(Intent(this, BookingAntreanActivity::class.java))
        }

        binding.btnRiwayatBerobat.setOnClickListener {
            startActivity(Intent(this, RiwayatAntreanActivity::class.java))
        }

        binding.btnInfoDokter.setOnClickListener {
            Toast.makeText(this, "Fitur Daftar Dokter segera hadir!", Toast.LENGTH_SHORT).show()
        }

        binding.btnBeriRating.setOnClickListener {
            // 1. Inflate layout dialog_rating_pasien
            val dialogBinding = DialogRatingPasienBinding.inflate(layoutInflater)
            val builder = AlertDialog.Builder(this).setView(dialogBinding.root)
            val alertDialog = builder.create()

            val namaDokterList = mutableListOf<String>()
            val dataDokterList = mutableListOf<com.example.klinikku.data.model.Dokter>()

            // 2. Lakukan fetch data dari path Firebase users
            val usersRef = FirebaseDatabase.getInstance().getReference("users")
            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (userSnap in snapshot.children) {
                        val role = userSnap.child("role").getValue(String::class.java)
                        if (role == "dokter") {
                            val nik = userSnap.child("nik").getValue(String::class.java) ?: userSnap.key ?: ""
                            val nama = userSnap.child("nama").getValue(String::class.java) ?: "Dokter"
                            val poli = userSnap.child("poli").getValue(String::class.java) ?: "Umum"
                            
                            val dokterObj = com.example.klinikku.data.model.Dokter(
                                nik = nik,
                                nama = nama,
                                poli = poli
                            )
                            dataDokterList.add(dokterObj)
                            namaDokterList.add(nama)
                        }
                    }

                    if (dataDokterList.isEmpty()) {
                        Toast.makeText(this@DashboardPasienActivity, "Belum ada data dokter di sistem", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // 3. Pasang data dokter ke Spinner
                    val adapter = android.widget.ArrayAdapter(this@DashboardPasienActivity, android.R.layout.simple_spinner_dropdown_item, namaDokterList)
                    dialogBinding.spinnerPilihDokter.adapter = adapter

                    dialogBinding.spinnerPilihDokter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val dokterTerpilih = dataDokterList[position]
                            dialogBinding.tvPoliRating.text = "Poli: " + dokterTerpilih.poli

                            // 4. Ambil data ulasan lama (Mode Edit jika ada)
                            val ratingRef = FirebaseDatabase.getInstance().getReference("ulasan")
                                .child(dokterTerpilih.nik)
                                .child(sessionManager.getNik() ?: "")
                                
                            ratingRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snap: DataSnapshot) {
                                    val ulasanLama = snap.getValue(Ulasan::class.java)
                                    if (ulasanLama != null) {
                                        dialogBinding.ratingBar.rating = (ulasanLama.bintang ?: 0.0).toFloat()
                                        dialogBinding.etKomentar.setText(ulasanLama.komentar ?: "")
                                        dialogBinding.btnKirimUlasan.text = "Perbarui Ulasan"
                                    } else {
                                        dialogBinding.ratingBar.rating = 0f
                                        dialogBinding.etKomentar.setText("")
                                        dialogBinding.btnKirimUlasan.text = "Kirim Ulasan"
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })

                            // 5. Logika tombol kirim
                            dialogBinding.btnKirimUlasan.setOnClickListener {
                                val dataUlasan = Ulasan(
                                    id_antrean = "RATING_DIRECT_" + dokterTerpilih.nik,
                                    pasien_id = sessionManager.getNik() ?: "",
                                    nama_pasien = sessionManager.getNama() ?: "Pasien",
                                    bintang = dialogBinding.ratingBar.rating.toDouble(),
                                    komentar = dialogBinding.etKomentar.text.toString()
                                )
                                ratingRef.setValue(dataUlasan).addOnSuccessListener {
                                    Toast.makeText(this@DashboardPasienActivity, "Ulasan berhasil dikirim ke dr. ${dokterTerpilih.nama}!", Toast.LENGTH_SHORT).show()
                                    alertDialog.dismiss()
                                }
                            }
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                    
                    dialogBinding.btnBatal.setOnClickListener { alertDialog.dismiss() }
                    alertDialog.show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DashboardPasienActivity, "Gagal mengambil data dokter", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupRecyclerView() {
        binding.rvJanjiAktifPasien.layoutManager = LinearLayoutManager(this)
        adapterAntrean = AntreanRealtimeAdapter(emptyList()) { pendaftaran ->
            val status = pendaftaran.status ?: "Menunggu"
            Toast.makeText(this, "Status antrean: $status", Toast.LENGTH_SHORT).show()
        }
        binding.rvJanjiAktifPasien.adapter = adapterAntrean
    }

    private fun loadAntreanRealtime() {
        val nikPasien = sessionManager.getNik() ?: return
        val database = FirebaseDatabase.getInstance()
        val antreanRef = database.getReference("antrean")

        val formatTanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = formatTanggal.format(Date())

        antreanRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawList = mutableListOf<Pendaftaran>()
                for (data in snapshot.children) {
                    val pendaftaran = data.getValue(Pendaftaran::class.java)
                    if (pendaftaran != null) {
                        rawList.add(pendaftaran)
                    }
                }
                listAntreanPasien = rawList

                // Filter antrean hari ini dan milik pasien ini saja
                val filteredList = rawList.filter { pendaftaran ->
                    val matchPasien = pendaftaran.pasien_id == nikPasien
                    val matchTanggal = pendaftaran.tanggal_kunjungan == todayStr
                    val matchStatus = !pendaftaran.status.equals("selesai", ignoreCase = true)
                    matchPasien && matchTanggal && matchStatus
                }.sortedBy { it.jam_praktek ?: "" }

                if (filteredList.isNotEmpty()) {
                    binding.rvJanjiAktifPasien.visibility = View.VISIBLE
                    binding.layoutAntreanKosongPasien.visibility = View.GONE
                    adapterAntrean.updateData(filteredList)
                } else {
                    binding.rvJanjiAktifPasien.visibility = View.GONE
                    binding.layoutAntreanKosongPasien.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DASHBOARD_PASIEN", "Database error: ${error.message}")
            }
        })
    }
}
