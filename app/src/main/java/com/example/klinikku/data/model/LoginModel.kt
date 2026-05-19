package com.example.klinikku.data.model

// Data yang dikirim ke server
data class LoginRequest(
    val username: String,
    val password: String
)

// Respon utama dari server
data class LoginResponse(
    val status: String,
    val message: String,
    val data: LoginData? = null
)

// Detail data user yang sukses login
data class LoginData(
    val nik: String,
    val nama: String,
    val role: String
)