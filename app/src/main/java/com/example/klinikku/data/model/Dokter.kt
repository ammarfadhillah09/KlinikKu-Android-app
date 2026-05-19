package com.example.klinikku.data.model

import com.google.gson.annotations.SerializedName

data class Dokter(
    val nik: String = "",
    val nama: String = "",
    val poli: String = "",
    val username: String = "",
    val password: String = "",
    val spesialis: String = "",
    val hari: String = "",
    @SerializedName("jam_mulai")  val jam_mulai: String = "",
    @SerializedName("jam_selesai") val jam_selesai: String = "",
    // id_jadwal is returned from API for edit/delete; not sent during POST
    @SerializedName("id_jadwal")  val id_jadwal: String = ""
)
