package com.example.klinikku.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Dokter

/**
 * Adapter for the patient-facing doctor listing screen (PendaftaranKlinikActivity).
 * Shows doctor name, specialist, and their schedule. Each card has a "Daftar" button.
 */
class DokterPasienAdapter(
    private var listDokter: List<Dokter>,
    private val targetBookingDay: String,
    private val onDaftarClick: (Dokter) -> Unit
) : RecyclerView.Adapter<DokterPasienAdapter.DokterPasienViewHolder>() {

    class DokterPasienViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInisial: TextView = view.findViewById(R.id.tvInisialDokterPasien)
        val tvNama: TextView = view.findViewById(R.id.tvNamaDokterPasien)
        val tvSpesialis: TextView = view.findViewById(R.id.tvSpesialisDokterPasien)
        val tvJadwal: TextView = view.findViewById(R.id.tvJadwalDokterPasien)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusPraktek)
        val btnDaftar: Button = view.findViewById(R.id.btnDaftarDokter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DokterPasienViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dokter_pasien, parent, false)
        return DokterPasienViewHolder(view)
    }

    override fun onBindViewHolder(holder: DokterPasienViewHolder, position: Int) {
        val dokter = listDokter[position]
        val ctx = holder.itemView.context

        // 1. Initials avatar (first 2 chars of name, skipping "Dr." prefix)
        try {
            val namePart = dokter.nama.removePrefix("Dr. ").removePrefix("dr. ").trim()
            val initials = namePart.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
            holder.tvInisial.text = initials.ifBlank { "Dr" }
        } catch (e: Exception) {
            holder.tvInisial.text = "Dr"
        }

        // 2. Name and specialist
        holder.tvNama.text = dokter.nama.ifBlank { "-" }
        holder.tvSpesialis.text = dokter.spesialis.ifBlank { "Dokter Umum" }

        // 3. Schedule display: "Jadwal: Senin, Selasa (08:00 - 17:00)"
        try {
            val jadwalText = buildString {
                append("Jadwal: ")
                if (dokter.hari.isNotBlank()) {
                    append(dokter.hari)
                } else {
                    append("-")
                }
                if (dokter.jam_mulai.isNotBlank() && dokter.jam_selesai.isNotBlank()) {
                    append(" (${dokter.jam_mulai} - ${dokter.jam_selesai})")
                }
            }
            holder.tvJadwal.text = jadwalText
        } catch (e: Exception) {
            holder.tvJadwal.text = "Jadwal: -"
        }

        // 4. Today's practice status badge (evaluated relative to the selected visit day)
        try {
            val daysList = dokter.hari.split(",").map { it.trim().lowercase() }
            val isPracticing = daysList.contains(targetBookingDay.trim().lowercase())
            if (isPracticing) {
                holder.tvStatus.text = "● Praktek Hari Kunjungan"
                holder.tvStatus.setTextColor(ctx.getColor(android.R.color.holo_green_dark))
            } else {
                holder.tvStatus.text = "○ Tidak Praktek Hari Kunjungan"
                holder.tvStatus.setTextColor(ctx.getColor(android.R.color.darker_gray))
            }
        } catch (e: Exception) {
            holder.tvStatus.text = "○ Cek Jadwal"
            holder.tvStatus.setTextColor(ctx.getColor(android.R.color.darker_gray))
        }

        // 5. Daftar button click
        holder.btnDaftar.setOnClickListener { onDaftarClick(dokter) }
    }

    override fun getItemCount(): Int = listDokter.size

    fun updateData(newList: List<Dokter>) {
        listDokter = newList
        notifyDataSetChanged()
    }
}
