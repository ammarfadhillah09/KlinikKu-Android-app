package com.example.klinikku.data.model

data class Ulasan(
    val id_antrean: String? = "",
    val pasien_id: String? = "",
    val nama_pasien: String? = "",
    val bintang: Double? = 0.0,
    val komentar: String? = ""
)