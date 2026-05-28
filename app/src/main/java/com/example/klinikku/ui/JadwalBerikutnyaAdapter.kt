package com.example.klinikku.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.klinikku.R
import com.example.klinikku.data.model.Pendaftaran
import com.example.klinikku.databinding.ItemJadwalBerikutnyaBinding

/**
 * JadwalBerikutnyaAdapter
 *
 * Adapter RecyclerView untuk menampilkan daftar jadwal pasien berikutnya
 * di halaman Dashboard Dokter. Data diambil real-time dari Firebase
 * node "pendaftaran", difilter berdasarkan dokter_id dan tanggal hari ini.
 */
class JadwalBerikutnyaAdapter(
    private var listPendaftaran: List<Pendaftaran>
) : RecyclerView.Adapter<JadwalBerikutnyaAdapter.JadwalViewHolder>() {

    class JadwalViewHolder(val binding: ItemJadwalBerikutnyaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JadwalViewHolder {
        val binding = ItemJadwalBerikutnyaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JadwalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JadwalViewHolder, position: Int) {
        val pendaftaran = listPendaftaran[position]

        with(holder.binding) {
            // Waktu praktek (ambil jam_praktek, fallback ke "-")
            tvJamPraktek.text = (pendaftaran.jam_praktek ?: "").ifBlank { "-" }

            // Nama pasien
            tvNamaPasien.text = (pendaftaran.nama_pasien ?: "").ifBlank { "Tanpa Nama" }

            // Jenis konsultasi (dari field poli)
            tvJenisKonsultasi.text = "Poli ${(pendaftaran.poli ?: "").ifBlank { "Umum" }}"

            // Badge status — warna berbeda berdasarkan status
            val status = (pendaftaran.status ?: "").lowercase()
            when {
                status.contains("selesai") -> {
                    tvBadgeStatus.text = "Selesai"
                    tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(root.context, R.color.text_secondary)
                    )
                }
                status.contains("menunggu") || status.contains("terjadwal") -> {
                    tvBadgeStatus.text = root.context.getString(R.string.badge_terjadwal)
                    tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(root.context, R.color.badge_green)
                    )
                }
                else -> {
                    tvBadgeStatus.text = root.context.getString(R.string.badge_menunggu)
                    tvBadgeStatus.setTextColor(
                        ContextCompat.getColor(root.context, R.color.metric_orange)
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int = listPendaftaran.size

    /**
     * Update data adapter secara dinamis dari Firebase listener.
     */
    fun updateData(newList: List<Pendaftaran>) {
        listPendaftaran = newList
        notifyDataSetChanged()
    }
}
