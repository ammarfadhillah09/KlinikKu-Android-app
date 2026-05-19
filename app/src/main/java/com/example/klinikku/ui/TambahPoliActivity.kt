package com.example.klinikku.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.klinikku.R
import com.example.klinikku.data.model.Poli
import com.example.klinikku.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TambahPoliActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambah_poli)

        val etNama = findViewById<EditText>(R.id.etNamaPoli)
        val etDeskripsi = findViewById<EditText>(R.id.etDeskripsiPoli)
        val btnSimpan = findViewById<Button>(R.id.btnSimpanPoli)

        btnSimpan.setOnClickListener {
            val nama = etNama.text.toString().trim()
            val deskripsi = etDeskripsi.text.toString().trim()

            if (nama.isNotEmpty() && deskripsi.isNotEmpty()) {
                val poliBaru = Poli(nama_poli = nama, deskripsi = deskripsi)
                simpanPoli(poliBaru, btnSimpan)
            } else {
                Toast.makeText(this, "Nama dan Deskripsi tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun simpanPoli(poli: Poli, btn: Button) {
        btn.isEnabled = false
        btn.text = "Menyimpan..."

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val response = RetrofitClient.instance.addPoli(poli)
                if (response.isSuccessful) {
                    Toast.makeText(this@TambahPoliActivity, "Poli Berhasil Ditambahkan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TambahPoliActivity, "Gagal simpan: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TambahPoliActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btn.isEnabled = true
                btn.text = "SIMPAN POLI"
            }
        }
    }
}
