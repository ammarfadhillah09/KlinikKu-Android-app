package com.example.klinikku.data.model

data class Pendaftaran(
    val id_antrean: String? = null,
    val pasien_id: String,
    val nama_pasien: String,
    val dokter_id: String,
    val nama_dokter: String,
    val poli: String,
    val tanggal_kunjungan: String,
    val jam_praktek: String,
    val nomor_antrean: Int,
    val status: String,

    // Field Tambahan untuk Hasil Berobat
    val diagnosa: String? = null,
    val obat: String? = null,
    val catatan_dokter: String? = null
)