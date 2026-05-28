# KlinikKu - Android Application (Production Ready)

Aplikasi manajemen antrean, pendaftaran pasien, dan rekam medis klinik berbasis Android yang terintegrasi secara *realtime* dengan Firebase Realtime Database Cloud dan infrastruktur modern.

🚀 **STATUS PROYEK: PRODUCTION READY / FULLY IMPLEMENTED**
Aplikasi ini telah menyelesaikan seluruh tahap pengembangan inti dan siap digunakan untuk demonstrasi penuh secara *End-to-End* (E2E Testing). Seluruh aktor (Pasien, Admin, dan Dokter) telah diimplementasikan 100% secara fisik di sisi Android Studio.

---

## 🔥 Fitur Unggulan yang Telah Diimplementasikan

### 1. Aktor Pasien & Validasi Kuota Dinamis (100% Tuntas)
* **Anti-Double Booking:** Pasien diproteksi ketat agar tidak dapat mendaftar lebih dari satu kali pada tanggal, sesi, dan dokter spesialis yang sama.
* **Pembatasan Kuota Mingguan:** Validasi otomatis berbasis minggu kunjungan kalender, mencegah satu pasien melakukan pendaftaran lebih dari 4 kali dalam satu minggu.
* **Proteksi Pembatalan (Cancel H-1):** Tombol batal antrean akan mengunci otomatis secara kondisional. Pembatalan janji temu hanya diizinkan maksimal H-1 sebelum tanggal kunjungan untuk menjaga efisiensi kuota klinik.

### 2. Aktor Dokter & Rekam Medis Realtime (100% Tuntas)
* **Dashboard Antrean Realtime:** Sinkronisasi instan data pasien aktif dari Firebase. Dokter dapat memantau antrean masuk hari ini secara langsung.
* **Manajemen Pemeriksaan (Timer):** Dilengkapi panel pemeriksaan aktif dengan kontrol tombol *Next* dan *Selesai* untuk alur pelayanan yang efisien.
* **Input Diagnosa & Resep Obat:** Dokter dapat menginput hasil pemeriksaan (diagnosa dan resep obat) secara dinamis yang langsung mengunci aman ke dalam riwayat medis pasien tanpa adanya data kosong (`-`).
* **UI/UX Estetik Premium:** Optimalisasi visual tingkat tinggi dengan menyembunyikan ActionBar bawaan, menghadirkan layout *full-screen* modern, dan menanamkan tombol logout kustom (ikon putih 24dp) secara manis tepat di dalam *Card* Profil Dokter.

### 3. Modul Kepuasan & Ulasan Pasien (100% Tuntas)
* **Dialog Rating Ala Gojek:** Pasien yang telah selesai diperiksa akan memunculkan dialog rating bintang interaktif dengan Spinner pilihan dokter dinamis.
* **Metrik Akumulasi Otomatis:** Setiap ulasan yang dikirim pasien langsung memperbarui akumulasi metrik rating dokter (misal: 4.5/5.0) secara *realtime* di dashboard utama dokter.

---

## 🛠️ Arsitektur Jaringan Cloud
Aplikasi ini menggunakan arsitektur hybrid modern dengan fokus pada kecepatan performa dan sinkronisasi data instan:
* **Database Utama:** Firebase Realtime Database Cloud (Direct-to-Firebase bypass untuk transaksi data medis krusial demi menghindari limitasi response).
* **Base URL API (Legacy/Auth):** `https://backend.ammaw.my.id/` (Next.js deployed on Vercel).

---

## 🔄 Alur Kerja Sistem (System Workflow)

1. **Pendaftaran & Proteksi (Android Client):**
   Saat pasien melakukan pendaftaran antrean, Android melalui logika lokal melakukan pengecekan validitas tanggal (H-1 pembatalan) dan memverifikasi kuota mingguan (maksimal 4x seminggu) sebelum mendorong data ke cloud.
   
2. **Sinkronisasi Instan (Firebase Realtime Cloud):**
   Begitu data pendaftaran lolos validasi, data tersimpan di Firebase dan secara instan (di bawah 1 detik) langsung menyembur dan memicu *update* pada `RecyclerView` Antrean Realtime di aplikasi milik Dokter.

3. **Input Medis & Finalisasi:**
   Dokter memeriksa pasien, mengaktifkan timer, dan menginput diagnosa serta obat. Setelah klik "Selesai", status antrean berubah secara global, dialog rating terpicu di sisi pasien, dan nilai rating dokter ter-update otomatis secara realtime.

---

## 👥 Akun Demo Uji Coba (Live Testing)

Untuk keperluan demonstrasi tanpa perlu registrasi ulang, Anda dapat login langsung menggunakan kredensial yang sudah matang di database berikut:

### 1. Akses Aktor Dokter (Dashboard Utama)
| Role / Akses | Nama Dokter | Spesialis |
| :--- | :--- | :--- |
| **Dokter (Utama)** | dr. Ammar S.PkK | Spesialis Kulit & Kelamin |

### 2. Akses Aktor Pasien (Demo Ready)
| Role / Akses | Username | Password |
| :--- | :--- | :--- |
| **Pasien Demo 1** | ammar | 123456 |
| **Pasien Demo 2** | firman | 123456 |

*Catatan: Anda juga dapat melakukan registrasi akun mandiri menggunakan NIK dummy yang telah didaftarkan sebelumnya di database cloud:*
* `1122334455667146`
* `1122334455667642`
* `1122334455667098`