# Konteks Sesi — Ekspor untuk Claude Code

Dibuat 15 September 2026, diperbarui 18 September 2026. File ini merangkum seluruh keputusan yang sudah diambil
sebelum pengerjaan kode dimulai, supaya sesi Claude Code tidak perlu mengulang
pembahasan yang sama atau mengambil arah yang berbeda.

Baca `CLAUDE.md` lebih dulu untuk gambaran proyek, lalu file ini untuk latar belakangnya.

---

## 1. Siapa dan apa

Hanif Fauzan Nurrahman, NIM 02032311025, S1 Teknik Informatika,
Fakultas Ilmu Komputer, Universitas Nasional Pasim.

Proposal Usulan Penelitian: **"Pengembangan Sistem Informasi Penjualan dan Manajemen
Persediaan Berbasis Web Menggunakan Metode Waterfall pada Usaha Zelatan Cell"**.

Objek: konter pulsa Zelatan Cell, Jl. Temugiri 01 Tinggarjati Lor, Desa Gentasari,
Kec. Kroya, Kab. Cilacap, Jawa Tengah 53282. Berdiri 2007.

Ini penelitian **pengembangan**, bukan rancang bangun. Sistem v1 sudah jadi dan dipakai;
yang diteliti adalah keterbatasannya dan penambahan fitur di atasnya. Jangan pernah
menulis kode atau dokumentasi yang mengasumsikan Zelatan Cell belum punya sistem.

---

## 2. Keputusan yang sudah final

Jangan ubah tanpa membicarakannya dengan Hanif lebih dulu.

| Keputusan | Pilihan | Alasan |
|---|---|---|
| Metode penelitian | Waterfall saja | Arah alternatif memakai Single Exponential Smoothing untuk peramalan stok sudah ditinggalkan. Abaikan file proposal bertanda "SES". |
| Integrasi Xendit | **Invoice API** (`POST /v2/invoices`) | Satu endpoint sudah menyediakan VA, QRIS, dan e-wallet sekaligus, dan hanya perlu satu callback. Pendekatan per-channel butuh tiga jalur API dan tiga callback — tidak sepadan untuk skala penelitian ini. |
| Mode Xendit | Sandbox / test | Batasan masalah proposal menyebut eksplisit tanpa transaksi uang riil. Secret key mode test diawali `xnd_development_`. |
| Urutan pengerjaan | Risiko tertinggi dulu | Xendit → Google OAuth2 → Email → Dashboard → PDF → Excel → Flash sale → Voucher → Tampilan tabel → Black Box Testing. Tiga integrasi eksternal diselesaikan lebih dulu karena paling rawan macet dan paling menentukan nilai kebaruan. |
| Login | Username/password tetap jadi jalur utama | Google OAuth2 adalah alternatif tambahan. Ini tertulis eksplisit di batasan masalah. |
| HTTP client | `RestClient` bawaan Spring | Spring Boot 3.4 sudah membawanya. Tidak perlu SDK Xendit pihak ketiga — lebih ringan dan lebih mudah dijelaskan di laporan. |
| Kredensial | File `.env` di root, tidak di-commit | `application.properties` hanya berisi placeholder `${...}`. |
| Frontend | **Thymeleaf → React + Vite** (18 Sep 2026) | Bertahap dan berdampingan, bukan rewrite sekaligus, supaya selalu ada versi yang bisa didemokan. |
| Basis data | **MySQL → PostgreSQL Supabase** (18 Sep 2026) | Supabase dipakai **hanya** sebagai Postgres terkelola. Bukan PostgREST, bukan Supabase Auth, bukan RLS. |
| Peran Spring Boot | Tetap jadi backend REST API | Ini yang menyelamatkan seluruh kode Java termasuk `XenditService`, menjaga Google OAuth2 dan email tetap di backend, dan membuat proposal tidak perlu dirombak. |
| Origin frontend | Satu origin dengan backend | Vite proxy saat pengembangan, `static/` saat produksi. Menghilangkan masalah CORS, cookie, dan CSRF lintas domain — dan yang terpenting membuat alur redirect Google OAuth2 standar tetap berfungsi. |
| Koneksi Supabase | Session pooler port 5432 | Koneksi langsung hanya IPv6 di plan gratis. Port 6543 tidak mendukung prepared statement, Hibernate butuh itu. |

---

## 3. Nilai kebaruan penelitian

Yang membedakan dari skripsi "rancang bangun SI penjualan" pada umumnya adalah
**integrasi tiga layanan eksternal ke dalam satu sistem informasi UMKM**:
payment gateway (Xendit), identity provider (Google OAuth 2.0), dan mail server (SMTP).
Ketiganya digambarkan sebagai aktor eksternal pada Gambar 3.3 proposal.

Dashboard analitik, ekspor PDF/Excel, dan flash sale adalah penguat, bukan inti kebaruan.
Kalau harus ada yang dikorbankan karena waktu, korbankan yang penguat — jangan yang inti.

---

## 4. Sistem v1 — apa yang sudah berjalan

Dipetakan dari 21 template Thymeleaf dan manifest tangkapan layar BAB IV proyek sebelumnya.

**Pelanggan:** registrasi, login username/password, lupa password, katalog produk,
transaksi pembelian memakai saldo, riwayat transaksi, kelola profil.

**Admin:** dashboard produk & stok, kelola produk, kelola kategori, kelola provider,
kelola data pengguna, permintaan top up dengan verifikasi manual, data top up,
kelola voucher diskon (baru sisi admin), laporan penjualan yang hanya tampil di layar.

Rute yang sudah ada: `/login`, `/register`, `/home-user`, `/home-admin`, `/topup`,
`/admin/topups`, `/admin/users`, `/admin/providers`, `/admin/vouchers`,
`/admin/transactions/sales`, `/categories`, `/profile-user`, `/user/edit`,
`/transaction/confirm/{id}`.

**Uji regresi wajib setelah setiap fitur baru**, terutama alur transaksi pembelian,
karena hampir semua fitur baru menyentuhnya.

---

## 5. Sepuluh item yang dikembangkan

| Kode | Fitur | Status di v1 |
|---|---|---|
| SKPL-F05 | Top up via payment gateway Xendit | Baru |
| SKPL-F03 | Login dengan akun Google (OAuth2) | Baru |
| SKPL-F18 | Notifikasi email otomatis (registrasi, top up, pembelian) | Baru |
| SKPL-F15 | Dashboard analitik Chart.js | Baru |
| SKPL-F16 | Cetak laporan PDF berkop toko | Baru |
| SKPL-F17 | Ekspor laporan Excel | Baru |
| SKPL-F14 | Kelola flash sale + countdown di beranda | Baru |
| SKPL-F04 / SKPL-F07 / SKPL-F13 | Voucher diskon tampil di beranda dan dipakai saat checkout | Pengembangan |
| SKPL-F19 | Perapian tampilan tabel | Pengembangan |
| — | Skenario Black Box Testing seluruh modul | Baru |

**Penting soal voucher:** halaman `/admin/vouchers` dan entity `Voucher` **sudah ada** di v1.
Yang belum ada adalah voucher tampil ke pelanggan dan bisa dipakai saat membeli.
Karena itu statusnya "Pengembangan", bukan "Baru". Jangan menulis seolah voucher dibuat
dari nol — penguji bisa membuka sistem v1 dan melihat halaman itu sudah ada.

---

## 6. Status kode saat ini

### Sudah dibuat, belum dikompilasi

Lima berkas di bawah ditulis dari sesi Cowork yang tidak punya akses Maven,
jadi **belum pernah lolos kompilasi**. Langkah pertama: `./mvnw compile`.

- `config/XenditProperties.java`
- `dto/request/XenditInvoiceRequest.java`
- `dto/response/XenditInvoiceResponse.java`
- `dto/request/XenditCallbackRequest.java`
- `services/XenditService.java`

Ditambah: blok `xendit.*` di `application.properties`,
`spring.config.import=optional:file:./.env[.properties]`, `.env.example` di root,
dan `.env` pada `.gitignore`.

Field permintaan dan webhook sudah dicocokkan ke dokumentasi resmi Xendit, bukan dari
ingatan: hanya `external_id` dan `amount` yang wajib, dan header verifikasi webhook
persisnya bernama `X-CALLBACK-TOKEN`.

### Belum dibuat

Lihat daftar tujuh langkah di `CLAUDE.md` bagian "Status terkini",
rinciannya di `docs/RENCANA-PENGEMBANGAN.md` bagian 1.

### Yang belum sempat dibaca

Sesi Cowork tidak bisa membaca isi berkas `.java` karena batas kedalaman folder.
Sebelum mengubah apa pun, **baca dulu** berkas berikut — spesifikasi di
`docs/RENCANA-PENGEMBANGAN.md` disusun tanpa melihat isinya, jadi bisa saja meleset:

- `models/TopUp.java` — struktur kolom saat ini
- `models/Voucher.java` — hanya 540 byte, kemungkinan masih sangat sederhana
- `enums/TransactionStatus.java` — apakah dipakai bersama oleh top up dan pembelian
- `config/SecurityConfig.java` — bentuk rantai filter dan aturan otorisasi
- `services/TopUpService.java` dan `controllers/TopUpController.java` — alur top up manual
- `dto/request/PaymentRequest.java`, `PaymentCallbackRequest.java`,
  `dto/response/PaymentResponse.java` — sisa rintisan payment gateway yang belum dipakai

---

## 7. Empat jebakan yang sudah diidentifikasi

1. **Verifikasi `X-CALLBACK-TOKEN` di webhook.** Tanpa ini, siapa pun yang tahu alamat
   endpoint callback bisa mengirim callback palsu dan menambah saldo sendiri.
   `XenditService.isValidCallbackToken()` sudah menyediakannya — tinggal dipakai.
   Saldo ditambah **hanya** di jalur callback, tidak pernah di `success_redirect_url`,
   karena URL itu bisa dibuka manual oleh siapa saja.

2. **Callback harus idempoten.** Xendit bisa mengirim callback yang sama lebih dari
   sekali. Kalau top up sudah berstatus lunas, jangan tambah saldo lagi — balas 200
   dan berhenti. Cocokkan juga `paid_amount` dengan nominal yang tersimpan.

3. **Harga modal harus tersimpan di transaksi**, bukan dibaca ulang dari `Product`
   saat laporan dibuat. Kalau tidak, angka profit pada laporan lama ikut berubah setiap
   harga produk diubah. Periksa `TransactionHistory` — kalau kolom itu belum ada,
   tambahkan sebelum mengerjakan dashboard (SKPL-F15).

4. **Flash sale dan voucher bisa berlaku bersamaan.** Tentukan urutannya sejak awal —
   disarankan harga flash sale dihitung dulu, baru voucher diterapkan ke hasilnya —
   dan catat keputusan itu, karena hampir pasti ditanya penguji.

---

## 8. Menguji webhook di localhost

Xendit tidak bisa menghubungi `localhost`. Dua cara:

- jalankan tunnel (ngrok atau sejenisnya), pasang URL publiknya sebagai webhook URL
  di dashboard Xendit dan sebagai `APP_BASE_URL` di `.env`; atau
- panggil sendiri endpoint callback memakai `curl` atau Postman dengan body tiruan
  dan header `X-CALLBACK-TOKEN` yang benar.

Cara kedua sudah cukup sebagai bukti Black Box Testing, asalkan di laporan ditulis
terus terang bahwa pengujian memakai simulasi callback, bukan pembayaran sandbox penuh.

---

## 9. Status dokumen proposal

Berkas ada di `C:\Users\ThinkPad\Documents\mas hanif\PROPOSAL UP\`:

- `BAB I - Proposal UP Waterfall Zelatan Cell.docx` — lengkap, Tabel 1.1 jadwal terisi
- `BAB II - Proposal UP Waterfall Zelatan Cell.docx` — lengkap, 2.1 s/d 2.18
- `BAB III - Proposal UP Waterfall Zelatan Cell.docx` — lengkap, 3 gambar + 10 tabel
- `DAFTAR PUSTAKA - Proposal UP Waterfall Zelatan Cell.docx` — 21 entri, semua tersitasi
- `gambar BAB III\` — sumber PNG ketiga diagram UML

Kode SKPL-F01 s/d F19 di dokumentasi proyek ini mengacu ke **Tabel 3.6** BAB III.
Perbandingan sistem lama dan baru ada di **Tabel 3.10**. Use case sistem usulan ada di
**Gambar 3.3**. Kalau implementasi menyimpang dari yang tertulis, catat di tabel paling
bawah `docs/CHECKLIST-PROGRES.md` supaya BAB III bisa disusulkan sebelum sidang.

### Dua hal di BAB I yang masih menunggu keputusan Hanif

Belum diperbaiki karena butuh persetujuannya:

1. **Latar Belakang paragraf 2** masih menyebut operasional Zelatan Cell "masih dilakukan
   secara manual", padahal paragraf berikutnya menyatakan sistem sudah ada. Dua kalimat
   itu saling bertentangan.

2. **Identifikasi Masalah poin 1–3 dan Tujuan Penelitian poin 1–2** masih berupa masalah
   era rancang bangun — mengatasi pencatatan manual, memantau stok real-time,
   menyederhanakan pembuatan laporan. Ketiganya **sudah diselesaikan v1**, jadi tidak lagi
   sah sebagai masalah penelitian pengembangan. Abstrak juga masih menyebut ketiganya.

Kalau Hanif menyinggung soal ini, usulannya: ubah poin 1–3 menjadi masalah keterbatasan
sistem berjalan (misalnya "Bagaimana mempercepat pengisian saldo yang masih bergantung
pada verifikasi manual admin?") tanpa mengubah jumlah poin, sehingga penomoran di seluruh
dokumen tetap utuh.

---

## 10. Prompt pembuka untuk sesi Claude Code

Salin ini sebagai pesan pertama:

> Saya melanjutkan pengembangan sistem informasi penjualan Zelatan Cell.
> Baca `CLAUDE.md`, `docs/KONTEKS-SESI.md`, `docs/RENCANA-PENGEMBANGAN.md`,
> dan `docs/CHECKLIST-PROGRES.md` lebih dulu.
>
> Jalankan `./mvnw compile` untuk memastikan lima berkas Xendit yang sudah ada
> benar-benar terkompilasi, perbaiki kalau ada yang salah.
>
> Lalu baca `models/TopUp.java`, `enums/TransactionStatus.java`,
> `config/SecurityConfig.java`, `services/TopUpService.java`,
> `controllers/TopUpController.java`, serta ketiga DTO `Payment*` yang belum dipakai.
> Laporkan isinya dan beri tahu di mana spesifikasi di `docs/RENCANA-PENGEMBANGAN.md`
> bagian 1 perlu disesuaikan dengan keadaan kode yang sebenarnya.
>
> Setelah itu baru lanjutkan SKPL-F05 sampai selesai: kolom baru di `TopUp`,
> `PaymentService`, `PaymentController`, `XenditWebhookController`, pengecualian CSRF
> untuk endpoint callback, dan perubahan `top_up.html`.
> Jangan merusak alur top up manual yang lama — biarkan tetap ada sebagai cadangan.
