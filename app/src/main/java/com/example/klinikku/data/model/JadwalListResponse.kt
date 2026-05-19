package com.example.klinikku.data.model

data class JadwalListResponse(
    val status: String,
    val message: String,
    val data: List<Jadwal>?
)
