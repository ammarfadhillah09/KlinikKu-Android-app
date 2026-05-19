package com.example.klinikku.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Poli

class PoliAdapter(private var listPoli: List<Poli>) :
    RecyclerView.Adapter<PoliAdapter.PoliViewHolder>() {

    class PoliViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaPoli: TextView = view.findViewById(R.id.tvNamaPoli)
        val tvDeskripsiPoli: TextView = view.findViewById(R.id.tvDeskripsiPoli)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PoliViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poli, parent, false)
        return PoliViewHolder(view)
    }

    override fun onBindViewHolder(holder: PoliViewHolder, position: Int) {
        val poli = listPoli[position]
        holder.tvNamaPoli.text = poli.nama_poli
        holder.tvDeskripsiPoli.text = poli.deskripsi
    }

    override fun getItemCount(): Int = listPoli.size

    fun updateData(newList: List<Poli>) {
        listPoli = newList
        notifyDataSetChanged()
    }
}
