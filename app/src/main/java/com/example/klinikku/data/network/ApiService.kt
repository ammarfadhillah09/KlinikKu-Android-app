package com.example.klinikku.data.network

import com.example.klinikku.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // 1. Fungsi Login
    @POST("api/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    // 2. Fungsi Cek NIK (Verifikasi Master Data Klinik)
    @POST("api/cek-nik")
    suspend fun cekNik(@Body request: CekNikRequest): Response<CekNikResponse>

    // 3. Fungsi Registrasi (Simpan ke node 'users' dan 'pasien')
    @POST("api/pasien")
    suspend fun registerPasien(@Body pasien: Pasien): Response<PasienResponse>

    // 4. Fungsi Ambil List Semua Pasien (Tampilan MainActivity)
    @GET("api/pasien")
    suspend fun getPasien(): Response<PasienListResponse>

    // 5. Fungsi Ambil Detail Satu Pasien (Berdasarkan NIK)
    @GET("api/pasien/detail")
    suspend fun getDetailPasien(@Query("nik") nik: String): Response<PasienResponse>

    // 6. Fungsi Hapus Pasien (Sprint 3)
    @DELETE("api/pasien/detail")
    suspend fun deletePasien(@Query("nik") nik: String): Response<PasienResponse>

    // 7. Fungsi Tambah Master Pasien (Sprint 4 - Admin Only)
    @POST("api/master-pasien")
    suspend fun addMasterPasien(@Body pasien: Pasien): Response<PasienResponse>

    // 8. Fitur Profil Medis (GET & POST)
    @GET("api/pasien/profil-medis")
    suspend fun getProfilMedis(@Query("nik") nik: String): Response<ProfilMedisResponse>

    @POST("api/pasien/profil-medis")
    suspend fun saveProfilMedis(@Body profil: ProfilMedis): Response<ProfilMedisResponse>

    // 9. Fitur Pendaftaran Antrean (Sprint Booking Antrean)
    @POST("api/pasien/pendaftaran")
    suspend fun daftarAntrean(@Body request: PendaftaranRequest): Response<PendaftaranResponse>

    // 10. Fitur Tambah Akun Dokter (Admin Only)
    @POST("api/admin/dokter")
    suspend fun addDokter(@Body dokter: Dokter): Response<PasienResponse>

    // 11. Ambil List Dokter (Sprint Daftar Dokter)
    @GET("api/admin/dokter")
    suspend fun getDokter(): Response<DokterListResponse>

    // 15. Update Data Dokter (Edit Mode)
    @PUT("api/admin/dokter/{nik}")
    suspend fun updateDokter(
        @Path("nik") nik: String,
        @Body dokter: Dokter
    ): Response<PasienResponse>

    // 16. Hapus Data Dokter (beserta jadwal-nya)
    @DELETE("api/admin/dokter/{nik}")
    suspend fun deleteDokter(
        @Path("nik") nik: String,
        @Query("id_jadwal") idJadwal: String
    ): Response<PasienResponse>

    // 12. CRUD Poli (Sprint Poli)
    @GET("api/admin/poli")
    suspend fun getPoli(): Response<PoliListResponse>

    @POST("api/admin/poli")
    suspend fun addPoli(@Body poli: Poli): Response<PoliSingleResponse>

    // 13. Kelola Jadwal Dokter (Sprint Jadwal Admin)
    @POST("api/admin/jadwal")
    suspend fun addJadwal(@Body jadwal: Jadwal): Response<JadwalSingleResponse>

    @GET("api/admin/jadwal")
    suspend fun getJadwal(): Response<JadwalListResponse>

    // 14. Cek Antrean Aktif Pasien (Sprint Monitoring Antrean)
    @GET("api/pasien/antrean-aktif")
    suspend fun getAntreanAktif(@Query("nik") nik: String): Response<AntreanAktifResponse>

    // 17. Riwayat Pendaftaran
    @GET("api/pasien/pendaftaran/riwayat/{nik}")
    suspend fun getRiwayatPendaftaran(@Path("nik") nik: String): Response<List<Pendaftaran>>

    // 18. Batalkan Antrean
    @POST("api/pasien/antrean/cancel")
    suspend fun cancelAntrean(@Body body: CancelRequest): Response<GenericResponse>
}