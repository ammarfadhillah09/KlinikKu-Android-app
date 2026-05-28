package com.example.klinikku.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Pendaftaran
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RiwayatAntreanAdapter(
    private val dataList: List<Pendaftaran>,
    private val onCancelClick: (String) -> Unit
) : RecyclerView.Adapter<RiwayatAntreanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPoli: TextView = view.findViewById(R.id.tvRiwayatPoli)
        val tvStatus: TextView = view.findViewById(R.id.tvRiwayatStatus)
        val tvDokter: TextView = view.findViewById(R.id.tvRiwayatDokter)
        val tvTanggal: TextView = view.findViewById(R.id.tvRiwayatTanggal)
        val tvNoAntrean: TextView = view.findViewById(R.id.tvRiwayatNoAntrean)
        val btnCancel: MaterialButton = view.findViewById(R.id.btnCancelAntrean)
        val layoutMedicalResults: View = view.findViewById(R.id.layoutMedicalResults)
        val tvKeluhan: TextView = view.findViewById(R.id.tvKeluhan)
        val tvDiagnosa: TextView = view.findViewById(R.id.tvDiagnosa)
        val tvObat: TextView = view.findViewById(R.id.tvObat)
        val tvCatatanDokter: TextView = view.findViewById(R.id.tvCatatanDokter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_antrean, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataList[position]

        holder.tvPoli.text = item.poli
        holder.tvDokter.text = item.nama_dokter ?: "-"
        holder.tvTanggal.text = item.tanggal_kunjungan
        holder.tvNoAntrean.text = item.nomor_antrean.toString()

        // Status badge styling
        holder.tvStatus.text = item.status
        when ((item.status ?: "").lowercase(Locale.ROOT)) {
            "pending", "menunggu" -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0")) // Orange light
                holder.tvStatus.setTextColor(Color.parseColor("#F57C00"))
            }
            "selesai" -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9")) // Green light
                holder.tvStatus.setTextColor(Color.parseColor("#388E3C"))
            }
            "dibatalkan" -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE")) // Red light
                holder.tvStatus.setTextColor(Color.parseColor("#D32F2F"))
            }
            else -> {
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E0E0E0")) // Grey light
                holder.tvStatus.setTextColor(Color.parseColor("#757575"))
            }
        }

        // H-1 Visual Guard
        val statusClean = (item.status ?: "").trim().lowercase(Locale.ROOT)

        if (statusClean == "menunggu" || statusClean == "pending") {
            // Default to visible first
            holder.btnCancel.visibility = View.VISIBLE
            holder.btnCancel.alpha = 1.0f
            holder.btnCancel.isEnabled = true
            holder.btnCancel.text = "BATALKAN ANTREAN"

            // Run H-1 Midnight Date calculations
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val targetDate = sdf.parse(item.tanggal_kunjungan) ?: java.util.Date()
                val today = sdf.parse(sdf.format(java.util.Date())) ?: java.util.Date()

                val diffInMillies = targetDate.time - today.time
                val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS)

                if (diffInDays < 1) {
                    // It's today or past, disable visually but keep item layout trackable
                    holder.btnCancel.alpha = 0.5f
                    holder.btnCancel.isEnabled = false
                    holder.btnCancel.text = "TIDAK BISA DIBATALKAN"
                }
            } catch (e: Exception) {
                // Safe fallback: keep it enabled if parsing fails so user isn't stuck
                holder.btnCancel.visibility = View.VISIBLE
                holder.btnCancel.alpha = 1.0f
                holder.btnCancel.isEnabled = true
                holder.btnCancel.text = "BATALKAN ANTREAN"
            }
        } else {
            // If status is "selesai" or "dibatalkan", hide it completely
            holder.btnCancel.visibility = View.GONE
        }

        // Bind Keluhan to appear outside/above medical results unconditionally
        holder.tvKeluhan.text = "Keluhan: " + (item.keluhan ?: "-")

        // Bind medical results if status is "selesai"
        if (item.status.equals("selesai", ignoreCase = true)) {
            holder.layoutMedicalResults.visibility = View.VISIBLE
            holder.tvDiagnosa.text = "Diagnosa Dokter: " + (item.diagnosa ?: "Belum diperiksa")
            holder.tvObat.text = "Obat: " + (item.obat ?: "-")
            holder.tvCatatanDokter.text = item.catatan_dokter ?: "-"
        } else {
            holder.layoutMedicalResults.visibility = View.GONE
        }

        holder.btnCancel.setOnClickListener {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val targetDate = sdf.parse(item.tanggal_kunjungan) ?: java.util.Date()
                val today = sdf.parse(sdf.format(java.util.Date())) ?: java.util.Date()

                val diffInMillies = targetDate.time - today.time
                val diffInDays = java.util.concurrent.TimeUnit.DAYS.convert(diffInMillies, java.util.concurrent.TimeUnit.MILLISECONDS)

                if (diffInDays <= 0) {
                    // JIKA DIKLIK PADA HARI-H ATAU SUDAH LEWAT, BLOKIR TOTAL!
                    android.widget.Toast.makeText(
                        holder.itemView.context, 
                        "Pembatalan gagal! Antrean hanya dapat dibatalkan maksimal H-1 sebelum tanggal kunjungan.", 
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    // JIKA MINIMAL H-1, BARU EKSEKUSI PEMBATALAN KE FIREBASE!
                    item.id_antrean?.let { id -> onCancelClick(id) }
                }
            } catch (e: Exception) {
                item.id_antrean?.let { id -> onCancelClick(id) }
            }
        }
    }

    override fun getItemCount(): Int = dataList.size
}
