package com.example.klinikku.data.model

data class PendaftaranRequest(
    val pasien_id: String,
    val nama_pasien: String,
    val poli: String,
    val tanggal_kunjungan: String,
    val id_jadwal: String
)

