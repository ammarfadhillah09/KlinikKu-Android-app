package com.example.klinikku.data.model

data class PasienListResponse(
    val status: String,
    val data: List<Pasien> // Mengambil list dari data class Pasien yang sudah ada
)