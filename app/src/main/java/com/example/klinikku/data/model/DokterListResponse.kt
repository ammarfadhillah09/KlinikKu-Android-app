package com.example.klinikku.data.model

data class DokterListResponse(
    val status: String,
    val message: String,
    val data: List<Dokter>?
)
