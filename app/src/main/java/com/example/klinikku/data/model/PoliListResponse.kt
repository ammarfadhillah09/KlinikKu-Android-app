package com.example.klinikku.data.model

data class PoliListResponse(
    val status: String,
    val message: String,
    val data: List<Poli>?
)
