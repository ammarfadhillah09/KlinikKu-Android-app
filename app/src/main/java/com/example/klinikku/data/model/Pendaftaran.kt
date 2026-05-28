package com.example.klinikku.data.model

data class Pendaftaran(
    val id_antrean: String? = "",
    val pasien_id: String? = "",
    val nama_pasien: String? = "",
    val dokter_id: String? = "",
    val nama_dokter: String? = "",
    val poli: String? = "",
    val tanggal_kunjungan: String? = "",
    val jam_praktek: String? = "",
    val nomor_antrean: Int? = 0,
    val status: String? = "",
    val keluhan: String? = "", // Field baru untuk keluhan berobat pasien

    // Field Tambahan untuk Hasil Berobat
    val diagnosa: String? = "",
    val obat: String? = "",
    val catatan_dokter: String? = ""
)