package com.example.klinikku.data.model

data class Jadwal(
    val id_jadwal: String? = null,
    val nik_dokter: String,
    val nama_dokter: String,
    val hari: String,
    val jam_mulai: String,
    val jam_selesai: String,
    val kuota: Int,
    val id_poli: String? = null
)
