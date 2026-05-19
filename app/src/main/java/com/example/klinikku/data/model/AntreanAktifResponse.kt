package com.example.klinikku.data.model

import com.google.gson.annotations.SerializedName

// Model detail tiket antrean aktif pasien
data class AntreanAktif(
    val token: String? = null,
    val nomor_antrean: Int = 0,
    val id_jadwal: String? = null,
    val nama_dokter: String? = null,
    val poli: String? = null,
    val status: String? = null,
    val tanggal: String? = null
)

// Wrapper respon dari endpoint GET api/pasien/antrean-aktif
data class AntreanAktifResponse(
    val status: String,
    val message: String,
    val data: AntreanAktif? = null
)
