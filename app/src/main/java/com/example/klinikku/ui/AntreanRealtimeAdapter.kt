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

class AntreanRealtimeAdapter(
    private var listAntrean: List<Pendaftaran>,
    private val onPeriksaClick: (Pendaftaran) -> Unit
) : RecyclerView.Adapter<AntreanRealtimeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomorUrut: TextView = view.findViewById(R.id.tvNomorUrut)
        val tvNamaPasienAntrean: TextView = view.findViewById(R.id.tvNamaPasienAntrean)
        val tvNikPasienAntrean: TextView = view.findViewById(R.id.tvNikPasienAntrean)
        val tvWaktuDaftarAntrean: TextView = view.findViewById(R.id.tvWaktuDaftarAntrean)
        val tvKeluhan: TextView = view.findViewById(R.id.tvKeluhan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_antrean_dokter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listAntrean[position]
        holder.tvNomorUrut.text = item.nomor_antrean.toString()
        holder.tvNamaPasienAntrean.text = item.nama_pasien
        holder.tvNikPasienAntrean.text = "NIK: ${item.pasien_id}"
        holder.tvKeluhan.text = "Keluhan: " + (item.keluhan ?: "-")

        var waktuDisplay = item.tanggal_kunjungan
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormat.parse(item.tanggal_kunjungan)
            if (date != null) {
                val outputFormat = SimpleDateFormat("EEEE, dd MMMM yyyy - 'Jam' HH:mm", Locale("id", "ID"))
                waktuDisplay = outputFormat.format(date)
            }
        } catch (e: Exception) {
            if (!item.jam_praktek.isNullOrBlank() && item.jam_praktek != "Menunggu") {
                waktuDisplay = "${item.tanggal_kunjungan} - ${item.jam_praktek}"
            }
        }

        holder.tvWaktuDaftarAntrean.text = "Waktu Kunjungan: $waktuDisplay"

        holder.itemView.setOnClickListener {
            onPeriksaClick(item)
        }
    }

    override fun getItemCount(): Int = listAntrean.size

    fun updateData(newList: List<Pendaftaran>) {
        listAntrean = newList
        notifyDataSetChanged()
    }
}