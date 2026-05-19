package com.example.klinikku.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Dokter

class DokterAdapter(
    private var listDokter: List<Dokter>,
    private val onEditClick: (Dokter) -> Unit,
    private val onDeleteClick: (Dokter) -> Unit
) : RecyclerView.Adapter<DokterAdapter.DokterViewHolder>() {

    class DokterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNamaDokter)
        val tvPoli: TextView = view.findViewById(R.id.tvPoliDokter)
        val tvJadwal: TextView = view.findViewById(R.id.tvJadwalDokter)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditDokter)
        val btnHapus: ImageButton = view.findViewById(R.id.btnHapusDokter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DokterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dokter, parent, false)
        return DokterViewHolder(view)
    }

    override fun onBindViewHolder(holder: DokterViewHolder, position: Int) {
        val dokter = listDokter[position]
        holder.tvNama.text = dokter.nama
        holder.tvPoli.text = "Poli: ${dokter.poli}"

        // Compose jadwal summary row – only show if data is available
        val jadwalText = buildString {
            if (dokter.hari.isNotBlank()) append(dokter.hari)
            if (dokter.jam_mulai.isNotBlank() && dokter.jam_selesai.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append("${dokter.jam_mulai} - ${dokter.jam_selesai}")
            }
        }
        holder.tvJadwal.text = if (jadwalText.isNotBlank()) jadwalText else "-"

        holder.btnEdit.setOnClickListener { onEditClick(dokter) }
        holder.btnHapus.setOnClickListener { onDeleteClick(dokter) }
    }

    override fun getItemCount(): Int = listDokter.size

    fun updateData(newList: List<Dokter>) {
        listDokter = newList
        notifyDataSetChanged()
    }
}
