package com.example.klinikku.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Pasien

class PasienAdapter(private var listPasien: List<Pasien>) :
    RecyclerView.Adapter<PasienAdapter.PasienViewHolder>() {

    class PasienViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNama)
        val tvNik: TextView = view.findViewById(R.id.tvNik)
        val tvDomisili: TextView = view.findViewById(R.id.tvDomisili)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasienViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pasien, parent, false)
        return PasienViewHolder(view)
    }

    override fun onBindViewHolder(holder: PasienViewHolder, position: Int) {
        val pasien = listPasien[position]
        holder.tvNama.text = pasien.nama
        holder.tvNik.text = "NIK: ${pasien.nik}"
        holder.tvDomisili.text = "Domisili: ${pasien.domisili}"

        // --- LOGIKA KLIK LISTENER ---
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailPasienActivity::class.java)

            // Kita kirim NIK sebagai ID Unik ke halaman detail
            intent.putExtra("NIK_PASIEN", pasien.nik)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = listPasien.size

    fun updateData(newList: List<Pasien>) {
        listPasien = newList
        notifyDataSetChanged()
    }
}