package com.example.klinikku.data.model


data class Pasien(
    val id: String,
    val nama: String,
    val nik: String,
    val domisili: String,
    val tanggal_daftar: String,
    // Username dan Password diberi nilai default kosong agar tidak error saat
    // mengambil data dari node 'pasien' yang memang tidak menyimpan password.
    val username: String = "",
    val password: String = ""
)