package com.example.klinikku.data.model


data class PasienResponse(
    val status: String,
    val message: String,
    // Field 'data' ini wajib ada agar DetailPasienActivity bisa membaca isi profil pasien
    val data: Pasien? = null
)