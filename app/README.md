# KlinikKu - Android Application (Prototype)

Aplikasi manajemen antrean dan rekam medis klinik berbasis Android yang terintegrasi dengan backend cloud Next.js dan Firebase Realtime Database.

⚠️ **STATUS PROYEK: PROTOTYPE / BELUM TUNTAS**
Aplikasi ini saat ini masih dalam tahap pengembangan aktif (*Work in Progress*).
* **Aktor Pasien & Admin:** Sudah tuntas dan dapat diuji secara live (koneksi jaringan cloud aktif).
* **Aktor Dokter:** *Fitur di sisi aplikasi Android belum tuntas/belum diimplementasikan*, namun arsitektur API di sisi Backend (Vercel) dan struktur tabel di Database (Firebase) sudah siap 100%.

---

## 🚀 Akun Demo Uji Coba (Live Testing)
Untuk keperluan pengujian atau demonstrasi aplikasi tanpa perlu melakukan registrasi baru, Anda dapat login menggunakan akun yang sudah terdaftar di database cloud kami berikut ini:

| Role / Akses | Username | Password |
| :--- |:---------|:---------|
| **Pasien (Demo)** | ammar    | 123456   |
| **Pasien (Demo)** | firman   | 123456   |

atau ingin mencoba daftar mandiri dengan nik yang sudah ada di daftarkan sebelumnya, jadi kalian bisa daftar username dan password sendiri berdasarkan nik nya


| Role / Akses | NIK (Dummy       | 
|:-------------|:-----------------|
| **Pasien**   | 1122334455667146 | 
| **Pasien**   | 1122334455667642 |
| **Pasien**   | 1122334455667098 | 
---

## 🛠️ Arsitektur Jaringan Cloud
Aplikasi ini sudah tidak menggunakan server lokal (`localhost`). Seluruh request HTTP/Retrofit diarahkan langsung ke infrastruktur *production* berikut:
* **Base URL API:** `https://backend.ammaw.my.id/`
* **Backend Platform:** Next.js deployed on Vercel (with rate limiting & strict CORS isolation).
* **Database:** Firebase Realtime Database cloud.

---

## 🔄 Alur Kerja Sistem (System Workflow)

Aplikasi KlinikKu ini berjalan sepenuhnya di atas infrastruktur cloud menggunakan arsitektur modern berbasis API. Berikut adalah alur transaksi datanya:

1. **Permintaan Pengguna (Android Client):**
   Saat pengguna melakukan aksi di aplikasi Android (seperti klik Login atau melihat antrean), Android melalui pustaka **Retrofit** mengirimkan request HTTPS ke domain publik: `https://backend.ammaw.my.id/`.

2. **Gerbang Keamanan & Validasi (Vercel Middleware):**
   Request tersebut diterima oleh server **Next.js di Vercel**. Sebelum diproses, request dilewatkan melalui *Middleware siber* untuk diperiksa batas batasnya (*Rate Limiting* maksimal 50 request/5 menit) dan penyaringan asal domain browser (*CORS Isolation*) demi mencegah serangan brute-force atau DDoS.

3. **Pemrosesan Data (Serverless Routing):**
   Jika lolos dari gerbang keamanan, rute backend Next.js (seperti `/api/login`) akan memproses data tersebut dan berkomunikasi langsung dengan **Firebase Realtime Database Cloud** untuk mencocokkan atau menyimpan data.

4. **Umpan Balik Instan (Response):**
   Firebase mengirimkan data kembali ke Next.js, lalu Next.js mengemasnya menjadi format **JSON** dan mengirimkannya kembali ke Android Studio. Aplikasi Android menerima data tersebut secara instan dan menampilkan dashboard (seperti nama "Ammar Ganteng" dan NIK) ke layar ponsel pengguna.

---

## 🛠️ Detail Infrastruktur Jaringan Cloud
* **Base URL API:** `https://backend.ammaw.my.id/`
* **Backend Platform:** Next.js Serverless (Deployed on Vercel).
* **Database Platform:** Firebase Realtime Database Cloud.