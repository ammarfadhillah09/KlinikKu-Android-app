package com.example.klinikku.data.model

data class CekNikRequest(
    val nik: String
)

data class CekNikResponse(
    val status: String,
    val message: String,
    val data: CekNikData?
)

data class CekNikData(
    val nama: String,
    val nik: String
)