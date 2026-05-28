package com.example.klinikku.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Pendaftaran
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AntreanDokterAdapter(
    private var listPasien: List<Pendaftaran>,
    private val onPeriksaClick: (Pendaftaran) -> Unit
) : RecyclerView.Adapter<AntreanDokterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomorUrut: TextView = view.findViewById(R.id.tvNomorUrut)
        val tvNamaPasienAntrean: TextView = view.findViewById(R.id.tvNamaPasienAntrean)
        val tvNikPasienAntrean: TextView = view.findViewById(R.id.tvNikPasienAntrean)
        val tvWaktuDaftarAntrean: TextView = view.findViewById(R.id.tvWaktuDaftarAntrean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_antrean_dokter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listPasien[position]

        holder.tvNomorUrut.text = item.nomor_antrean.toString()
        holder.tvNamaPasienAntrean.text = item.nama_pasien
        holder.tvNikPasienAntrean.text = "NIK: ${item.pasien_id}"

        var waktuDisplay = item.tanggal_kunjungan
        try {
            // Parsing format ISO dari backend Vercel (UTC)
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormat.parse(item.tanggal_kunjungan)
            if (date != null) {
                // Konversi ke format lokal Indonesia
                val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy - 'Jam' HH:mm", Locale("id", "ID"))
                waktuDisplay = outputFormat.format(date)
            }
        } catch (e: Exception) {
            // Fallback jika tipe data base bukan ISO string
            waktuDisplay = "${item.tanggal_kunjungan} - ${item.jam_praktek}"
        }

        holder.tvWaktuDaftarAntrean.text = "Waktu Daftar: $waktuDisplay"

        holder.itemView.setOnClickListener {
            onPeriksaClick(item)
        }
    }

    override fun getItemCount(): Int = listPasien.size

    fun updateData(newList: List<Pendaftaran>) {
        listPasien = newList
        notifyDataSetChanged()
    }
}