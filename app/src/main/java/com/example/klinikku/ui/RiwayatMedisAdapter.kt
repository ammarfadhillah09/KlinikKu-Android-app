package com.example.klinikku.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Pendaftaran

class RiwayatMedisAdapter(
    private val dataList: List<Pendaftaran>
) : RecyclerView.Adapter<RiwayatMedisAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaPasien: TextView = view.findViewById(R.id.tvNamaPasien)
        val tvPoli: TextView = view.findViewById(R.id.tvJenisKonsultasi)
        val tvTanggal: TextView = view.findViewById(R.id.tvJamPraktek) // we can reuse tvJamPraktek or style tvBadgeStatus
        val tvBadgeStatus: TextView = view.findViewById(R.id.tvBadgeStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal_berikutnya, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]
        holder.tvNamaPasien.text = item.nama_pasien
        holder.tvPoli.text = "Poli ${item.poli} - Diagnosa: ${item.diagnosa ?: "Pemeriksaan Rutin"}"
        holder.tvTanggal.text = item.tanggal_kunjungan
        holder.tvBadgeStatus.text = "Selesai"
        holder.tvBadgeStatus.setTextColor(0xFF388E3C.toInt())
        holder.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_green)
    }

    override fun getItemCount(): Int = dataList.size
}
