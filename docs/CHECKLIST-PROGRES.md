# Checklist Progres Pengembangan

Centang saat fitur selesai **dan** sudah diuji manual. Isi tanggal dan catatan
supaya bisa langsung disalin ke BAB IV.

Kode SKPL mengacu ke Tabel 3.6 BAB III. Kolom "Proposal" menunjuk bagian laporan
yang harus ikut diperbarui kalau ada keputusan teknis yang berubah saat coding.

> **Sudah dikerjakan (15 September 2026)** — lapisan klien Xendit dibuat dari sesi Cowork:
> `config/XenditProperties.java`, `dto/request/XenditInvoiceRequest.java`,
> `dto/request/XenditCallbackRequest.java`, `dto/response/XenditInvoiceResponse.java`,
> `services/XenditService.java`, ditambah blok `xendit.*` di `application.properties`,
> file `.env.example`, dan `.env` pada `.gitignore`.
> Berkas-berkas itu **belum pernah dikompilasi** karena sesi tersebut tidak punya akses
> Maven. Langkah pertama di mesin sendiri: jalankan `./mvnw compile`.


> **Pembaruan 25 September 2026** — status terkini ada di `CLAUDE.md` dan `../plan.md`.
> Daftar centang di bawah (Tahap A–G) disusun 15–18 Sep dan sebagian sudah usang:
> Thymeleaf dan MySQL sudah dihapus, database memakai Supabase **Projek-hanif**.
> Hasil uji yang sudah dijalankan dicatat di bagian "Hasil uji 25 Sep 2026" berikut.

## Hasil uji 25 Sep 2026

Cara uji: API dipanggil langsung (curl) dan lewat antarmuka React di browser; data di
Projek-hanif. "Unit" = unit test otomatis (`mvn test`), tanpa database.

| Kode | Skenario | Hasil yang diharapkan | Hasil |
|---|---|---|---|
| Top up manual | Pelanggan ajukan top up Rp 25.000 | Status PENDING, saldo belum bertambah | Sesuai |
| Top up manual | Admin menekan Setujui | Saldo +25.000 tepat sekali | Sesuai (250.000 → 275.000) |
| Top up manual | Setujui kedua kali pada top up yang sama | Ditolak, saldo tetap | Sesuai (HTTP 400) |
| Top up manual | Setujui top up Xendit yang belum dibayar | Ditolak | Sesuai (HTTP 400) |
| Top up manual | Akun pelanggan memanggil endpoint approve | Ditolak | Sesuai (HTTP 403) |
| F05 webhook | Callback PAID nominal cocok / dikirim dua kali / nominal beda / EXPIRED / externalId tidak dikenal | Saldo hanya bertambah sekali dan hanya jika cocok | Sesuai (Unit, 5 kasus) |
| Pembelian | 5 pembelian bersamaan, saldo cukup untuk 1 | 1 berhasil, 4 ditolak, saldo tidak minus, stok −1 | Sesuai |
| F04/F07/F13 | Voucher PROMOHEMAT dipakai pelanggan yang sama dua kali | Kedua ditolak; validasi voucher menampilkan "sudah pernah dipakai" | Sesuai |
| F14 | Simpan flash sale bertabrakan / harga ≥ normal / selesai sebelum mulai | Ditolak dengan pesan jelas | Sesuai (3 kasus) |
| OTP | 5 kali OTP salah | Kode dibatalkan, kode benar pun tidak berlaku lagi | Sesuai (Unit) |
| OTP | Minta OTP baru < 60 detik | Ditolak | Sesuai (Unit) |
| Keamanan | POST login tanpa header X-XSRF-TOKEN | Ditolak | Sesuai (HTTP 403) |
| Keamanan | Respons `/api/admin/users` | Tidak memuat password maupun OTP | Sesuai |
| Login | Login admin memakai email | Berhasil | Sesuai |
| F03 | Akun BANNED login lewat Google | Ditolak, status tetap BANNED | Sesuai (Unit) |
| F03 | Email Google sudah terdaftar / email baru | Ditautkan tanpa akun ganda / akun baru + username unik | Sesuai (Unit) |
| F21 rating | Pelanggan ≥ 2 transaksi membuka beranda | Popup "Nilai Kami" muncul, rating tersimpan di `store_ratings` | Sesuai (browser) |
| F21 rating | Bintang 0 atau 6 / komentar 501 karakter / pelanggan 0 transaksi / tanpa login | Ditolak (400/400/400/401) | Sesuai |
| F21 rating | Kirim ulang rating | Baris yang sama diperbarui, bukan bertambah | Sesuai |
| F15 | Dashboard admin | Kartu ringkasan, tren 30 hari, grafik 12 bulan, produk terlaris, kategori | Sesuai (browser) |
| F16 | Unduh PDF periode 1–30 Sep 2026 | Kop toko, periode, tabel, ringkasan; angka sama dengan layar | Sesuai (omset Rp 1.053.200, laba Rp 126.384) |
| F17 | Unduh Excel periode yang sama | Nominal bertipe angka, kolom laba berupa rumus, baris total `SUM` | Sesuai |
| F19 | Tabel laporan, top up, pengguna | Paginasi dan pencarian di server | Sesuai |
| F20 AI | Semua 8 menu laporan dijalankan pada database | Tanpa error | Sesuai |
| F20 AI | Output AI berisi SQL / laporan di luar menu / rentang > 366 hari / limit > 20 | Ditolak | Sesuai (Unit) |
| F20 AI | Chat tanpa `OPENROUTER_API_KEY` | Pesan "AI belum dikonfigurasi", sesi tetap tersimpan | Sesuai (browser) |
| Profil | Nomor HP salah format / milik akun lain / password lama salah | Ditolak | Sesuai |
| Profil | Ganti password benar, lalu login dengan password baru | Berhasil | Sesuai |
| Kategori/provider | Nama kosong / duplikat / hapus yang masih dipakai produk | Ditolak | Sesuai |
| Pembelian | Beli pulsa dengan nomor tujuan kosong / `12345` | Ditolak, saldo tidak berkurang | Sesuai (HTTP 400, saldo tetap 350.000) |
| Pembelian | Beli pulsa ke `+62 812-9999-0000` | Berhasil, nomor tersimpan sebagai `081299990000` | Sesuai |
| Pembelian | Beli token PLN dengan nomor meter 10 digit | Ditolak | Sesuai (HTTP 400) |
| Pembelian | Beli token PLN 20.000 ke meter `14234567890` | Berhasil, token 20 digit, nomor meter tampil di struk, riwayat, dan laporan admin | Sesuai |
| Pembelian | Katalog pelanggan | Hanya produk digital (pulsa, data, token PLN, voucher game); aksesoris dan kategori kosong tidak tampil | Sesuai (36 produk, 4 kategori) |
| Pembelian | Beli aksesoris lewat API | Ditolak "hanya dijual langsung di konter" | Sesuai (HTTP 400) |
| Pembelian | Beli Mobile Legends 86 Diamonds dengan ID kosong / `<script>` | Ditolak | Sesuai (HTTP 400) |
| Pembelian | Beli Mobile Legends 86 Diamonds ke `12345678 (1234)` | Berhasil, ID game tersimpan | Sesuai |
| Katalog | Beranda "Semua" | Hanya kartu kategori (Pulsa, Paket Data, Token PLN, Voucher Game) | Sesuai |
| Katalog | Pilih Pulsa, lalu Telkomsel | Kartu 6 operator, lalu 16 nominal Rp 2.000 – Rp 1.000.000; breadcrumb Semua › PULSA › Telkomsel | Sesuai |
| Katalog | Pilih Voucher Game / Token PLN | 5 kartu game / langsung 6 nominal token (hanya satu provider) | Sesuai |
| Username | Login `BUDI_SANTOSO` dan `Budi_Santoso` untuk akun `budi_santoso` | Berhasil, nama tampil sesuai ejaan asli | Sesuai |
| Username | Daftar `Budi_Santoso` dengan email lain | Ditolak "Username sudah digunakan" | Sesuai (HTTP 400) |
| Struk PLN | Pemilik membuka `/api/transactions/{id}/receipt` | Data struk | Sesuai (HTTP 200) |
| Struk PLN | Pelanggan lain / tamu membuka struk yang sama | Ditolak | Sesuai (HTTP 404 / 401) |
| Struk PLN | Halaman `/struk/{id}` dicetak | Lebar 58 mm, navbar dan footer tidak ikut tercetak | Sesuai (CSS `@page size 58mm`); belum dicoba ke printer fisik |
| Bayar Xendit | Pesan Token PLN 20.000 dengan metode Xendit | Invoice sandbox dibuat: harga Rp 21.500 + admin 10% Rp 2.150 = Rp 23.650 | Sesuai |
| Bayar Xendit | Webhook PAID nominal salah | Diabaikan, pesanan tetap PENDING | Sesuai |
| Bayar Xendit | Webhook PAID benar, lalu dikirim ulang | Produk diproses sekali (token 20 digit), saldo tidak berubah | Sesuai |
| Bayar Xendit | Stok habis setelah pelanggan membayar | Pesanan FAILED, seluruh pembayaran (Rp 3.300 termasuk admin) masuk saldo | Sesuai |
| Bayar Xendit | Invoice kedaluwarsa / pelanggan lain membuka pesanan / webhook tanpa token | EXPIRED / 404 / 401 | Sesuai |
| Profil | Ganti nama ke nama akun lain beda huruf / berisi `<b>` | Ditolak | Sesuai |
| Profil | Ganti nama pengguna | Sesi tetap aktif; login dengan nama baru berhasil, nama lama ditolak | Sesuai |
| Profil | Unggah foto PNG / file bukan gambar / hapus foto | JPEG 256 px tersimpan / ditolak / foto hilang | Sesuai |
| Katalog | Logo provider di kartu produk | Semua provider punya logo (`/api/providers/{id}/logo`) | Sesuai |
| Pembelian | Aturan nomor HP (10–13 digit, awalan 08/62/+62), meter PLN (11–12 digit), ID game (4–40 karakter) | Sesuai aturan | Sesuai (Unit, `PurchaseTargetTest`) |

**Belum diuji:** pembayaran Xendit sandbox penuh lewat ngrok, login Google sungguhan, email
terkirim ke alamat asli, dan AI chat dengan model OpenRouter sungguhan (menunggu API key).

---

## Urutan tahap

| Tahap | Isi |
|---|---|
| A | Migrasi database MySQL → Supabase PostgreSQL |
| B | Integrasi eksternal: SKPL-F05 Xendit, F03 Google OAuth2, F18 email |
| C | Lapisan REST API dan kerangka React (paralel dengan B ke atas) |
| D | Penyajian informasi: SKPL-F15 dashboard, F16 PDF, F17 Excel |
| E | Promosi dan tampilan: SKPL-F14 flash sale, voucher, F19 tabel |
| F | Pemindahan 21 halaman Thymeleaf ke React |
| G | Pengujian Black Box seluruh modul |

Penomoran ini sama dengan yang dipakai `docs/MIGRASI-REACT-SUPABASE.md`.

## Persiapan

- [ ] Backup basis data `flashcell_db` sebelum mulai
- [x] Proyek dijadikan repositori git (kalau belum) dan `.gitignore` memuat `.env`
- [ ] Buat file `.env` berisi kredensial, jangan di-commit
- [ ] Pastikan `mvnw spring-boot:run` masih jalan normal sebelum ada perubahan
- [ ] Catat daftar fitur v1 yang berfungsi sebagai acuan uji regresi

---

---

## Tahap A — Migrasi database MySQL → Supabase PostgreSQL

Dikerjakan **sebelum** menuntaskan SKPL-F05, supaya fitur-fitur berikutnya cukup
diuji satu kali. Rincian di `docs/MIGRASI-REACT-SUPABASE.md` Tahap A.

- [x] Tambah driver `org.postgresql:postgresql` di `pom.xml` (driver MySQL dibiarkan)
- [x] Buat `src/main/resources/application-supabase.properties`
- [x] Tambah `SUPABASE_DB_HOST`, `SUPABASE_DB_USER`, `SUPABASE_DB_PASSWORD` di `.env.example`
- [ ] Buat proyek Supabase, region Singapore (`ap-southeast-1`)
- [ ] Simpan password database (hanya ditampilkan sekali saat pembuatan proyek)
- [ ] Ambil host dan user dari Dashboard > Connect > **Session pooler**, isi ke `.env`

Perbaiki jebakan PostgreSQL sebelum menjalankan aplikasi:

- [ ] `models/User.java` — beri `@Table(name = "users")`; `user` adalah kata kunci PostgreSQL
- [ ] Periksa entity lain terhadap kata kunci: `order`, `group`, `session`, `transaction`
- [ ] Samakan strategi ID ke `GenerationType.IDENTITY` di semua entity
- [ ] Cari `nativeQuery = true` di semua repository, ganti sintaks khas MySQL
      (`LIMIT x,y`, `IFNULL`, `DATE_FORMAT`, backtick, `ON DUPLICATE KEY`)
- [ ] Periksa kolom boolean (`active` pada `Voucher`) — tinyint(1) menjadi boolean
- [ ] Pastikan tidak ada `@Column(name = ...)` dengan huruf besar di tengah

Pemindahan:

- [ ] Jalankan profil `supabase` dengan `ddl-auto=update`, biarkan Hibernate membentuk skema
- [ ] Periksa hasil skema di Supabase Table Editor
- [ ] Ekspor data MySQL ke CSV, impor ke Supabase
- [ ] Majukan semua sequence dengan `setval(pg_get_serial_sequence(...))`
- [ ] Ubah `ddl-auto` menjadi `validate` pada profil supabase
- [ ] Uji regresi seluruh fitur v1 dengan profil `supabase`
- [ ] Pastikan jumlah baris tiap tabel sama dengan di MySQL
- [ ] Pastikan profil default (MySQL) masih bisa dijalankan sebagai cadangan

Tanggal selesai: ______  Catatan: ______________________________________

---

---

## Tahap B — Integrasi eksternal (SKPL-F05, F03, F18)

Dikerjakan setelah Tahap A selesai, sehingga langsung di atas PostgreSQL.

### SKPL-F05 Top up via payment gateway Xendit
Proposal: Tabel 3.3, Tabel 3.5 (Performance, Control), Tabel 3.6, Tabel 3.10, Gambar 3.3

- [ ] Daftar akun Xendit, ambil secret API key mode **test/sandbox**
- [x] Tambah properti `xendit.*` di `application.properties`
- [ ] Tambah kolom baru di entity `TopUp`
- [x] Buat `XenditService` (RestClient, Basic Auth) — termasuk verifikasi X-CALLBACK-TOKEN
- [x] Buat DTO `XenditInvoiceRequest`, `XenditInvoiceResponse`, `XenditCallbackRequest`
- [ ] Jalankan `./mvnw compile` untuk memastikan berkas baru benar-benar terkompilasi
- [ ] Buat `PaymentService`
- [ ] Buat `PaymentController` — `POST /topup/pay`
- [ ] Buat `XenditWebhookController` — `POST /api/xendit/callback`
- [ ] Kecualikan endpoint callback dari CSRF dan autentikasi di `SecurityConfig`
- [ ] Verifikasi header `x-callback-token`
- [ ] Buat penambahan saldo bersifat idempoten
- [ ] Cocokkan `paid_amount` dengan nominal top up
- [ ] Ubah `top_up.html` — pilihan bayar otomatis
- [ ] Buat halaman status `/topup/status/{externalId}`
- [ ] Uji: top up berhasil, saldo bertambah otomatis
- [ ] Uji: callback dikirim dua kali, saldo tetap bertambah sekali
- [ ] Uji: callback tanpa token benar ditolak
- [ ] Uji: alur top up manual lama masih berfungsi
- [ ] Tangkapan layar untuk BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

### SKPL-F03 Login dengan akun Google
Proposal: BAB II 2.7, Tabel 3.5 (Control), Tabel 3.6, Tabel 3.10, Gambar 3.3

- [ ] Buat OAuth Client ID di Google Cloud Console
- [ ] Daftarkan redirect URI `http://localhost:8080/login/oauth2/code/google`
- [ ] Tambah dependency `spring-boot-starter-oauth2-client`
- [ ] Tambah kolom `email`, `authProvider`, `providerId`, `profilePictureUrl` di `User`
- [ ] Pastikan kolom `password` boleh null
- [ ] Buat `CustomOAuth2UserService`
- [ ] Buat `OAuth2LoginSuccessHandler` (arahkan sesuai role)
- [ ] Tambah `.oauth2Login()` di `SecurityConfig` tanpa menghapus `formLogin()`
- [ ] Tambah tombol Google di `login.html`
- [ ] Uji: akun Google baru otomatis terbuat dan bisa transaksi
- [ ] Uji: email yang sudah terdaftar tidak menghasilkan akun ganda
- [ ] Uji: akun Google yang coba login lokal ditolak dengan pesan jelas
- [ ] Uji: login username/password lama masih normal
- [ ] Tangkapan layar untuk BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

### SKPL-F18 Notifikasi email otomatis
Proposal: BAB II 2.8, Tabel 3.5 (Information), Tabel 3.6, Tabel 3.10, Gambar 3.3

- [ ] Aktifkan 2FA Gmail dan buat App Password
- [ ] Tambah dependency `spring-boot-starter-mail`
- [ ] Tambah properti `spring.mail.*` dan `app.mail.*`
- [ ] Buat `EmailService` dengan `@Async`
- [ ] Buat template `email/welcome.html`
- [ ] Buat template `email/topup-success.html`
- [ ] Buat template `email/purchase-receipt.html`
- [ ] Pasang pemicu di alur registrasi
- [ ] Pasang pemicu di alur top up berhasil
- [ ] Pasang pemicu di alur transaksi pembelian
- [ ] Bungkus semua pengiriman dengan try-catch
- [ ] (Opsional) Entity `EmailLog` untuk bukti di BAB IV
- [ ] Uji: ketiga email terkirim dan isinya sesuai data
- [ ] Uji: SMTP sengaja dimatikan, transaksi tetap berhasil
- [ ] Tangkapan layar email yang diterima untuk BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

---

---

## Tahap C — Lapisan REST API dan kerangka React

Berjalan paralel dengan pengerjaan fitur. Rincian di
`docs/MIGRASI-REACT-SUPABASE.md` Tahap C.

- [ ] Buat folder `frontend/` dengan Vite + React
- [ ] Atur `vite.config.js` — proxy `/api`, `/login`, `/oauth2` ke `localhost:8080`
- [ ] Aktifkan `CookieCsrfTokenRepository.withHttpOnlyFalse()` di `SecurityConfig`
- [ ] Pembungkus `fetch` yang otomatis mengirim header `X-XSRF-TOKEN`
- [ ] Susun kerangka routing React dan komponen layout dari `fragments/navbar.html`
- [ ] Pasang `chart.js` + `react-chartjs-2` (dipakai SKPL-F15)
- [ ] Buat REST controller berpasangan untuk tiap halaman yang dipindah,
      kembalikan DTO response, **jangan** entity JPA
- [ ] `frontend-maven-plugin` di `pom.xml` — kerjakan paling akhir

Tanggal selesai: ______  Catatan: ______________________________________

---

---

## Tahap D — Penyajian informasi (SKPL-F15, F16, F17)

### SKPL-F15 Dashboard analitik Chart.js
Proposal: BAB II 2.9, Tabel 3.2, Tabel 3.5 (Information), Tabel 3.6, Tabel 3.10

- [ ] Pastikan harga modal tersimpan di `TransactionHistory` saat transaksi
- [ ] Buat query agregasi di repository (`@Query`, bukan hitung di Java)
- [ ] Buat `DashboardService` dan DTO respons
- [ ] Buat `DashboardController` dengan endpoint JSON
- [ ] Kartu ringkasan: omset, profit, jumlah transaksi, pelanggan, stok menipis
- [ ] Grafik garis tren penjualan harian 30 hari
- [ ] Grafik batang penjualan bulanan 12 bulan
- [ ] Grafik 5 produk terlaris
- [ ] Grafik donat komposisi per kategori/provider
- [ ] Uji: angka cocok dengan hitungan manual dari tabel transaksi
- [ ] Tangkapan layar untuk BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

### SKPL-F16 Cetak laporan PDF
Proposal: BAB II 2.11, Tabel 3.4, Tabel 3.5 (Efficiency), Tabel 3.6, Tabel 3.10

- [ ] Tambah dependency OpenPDF
- [ ] Buat `ReportPdfService`
- [ ] Kop toko: nama, alamat lengkap, periode, tanggal cetak
- [ ] Tabel transaksi + baris ringkasan
- [ ] Endpoint `/admin/transactions/sales/pdf?start=&end=`
- [ ] Tombol cetak di `admin_sales_report.html`
- [ ] Uji: angka di PDF cocok dengan yang tampil di layar
- [ ] Uji: periode kosong tidak error
- [ ] Simpan contoh PDF untuk lampiran BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

### SKPL-F17 Ekspor laporan Excel
Proposal: BAB II 2.11, Tabel 3.4, Tabel 3.5 (Efficiency), Tabel 3.6, Tabel 3.10

- [ ] Tambah dependency `poi-ooxml`
- [ ] Buat `ReportExcelService`
- [ ] Header tebal, kolom auto-size, format rupiah numerik
- [ ] Baris total memakai formula `SUM`
- [ ] Endpoint `/admin/transactions/sales/excel?start=&end=`
- [ ] Tombol ekspor di `admin_sales_report.html`
- [ ] Uji: file terbuka di Excel, angka bisa dijumlahkan
- [ ] Simpan contoh file untuk lampiran BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

---

---

## Tahap E — Promosi dan tampilan (SKPL-F14, voucher, F19)

### SKPL-F14 Flash sale
Proposal: BAB II 2.10, Tabel 3.6, Tabel 3.10, Gambar 3.3

- [ ] Entity `FlashSale` + repository
- [ ] `FlashSaleService` dengan validasi periode dan kuota
- [ ] Halaman admin `/admin/flash-sales` (CRUD)
- [ ] Validasi periode tidak boleh bertabrakan untuk produk sama
- [ ] Bagian flash sale + countdown timer di `home_user.html`
- [ ] Harga flash sale dihitung di server saat transaksi
- [ ] Uji: countdown benar, lewat `endAt` harga kembali normal
- [ ] Uji: kuota habis menghentikan diskon
- [ ] Tangkapan layar untuk BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

### SKPL-F04 / SKPL-F07 / SKPL-F13 Voucher diskon di beranda dan checkout
Proposal: BAB II 2.10, Tabel 3.1 (baris Keterbatasan), Tabel 3.6, Tabel 3.10

- [ ] Lengkapi entity `Voucher` (tipe diskon, minimum, batas diskon, masa berlaku, kuota)
- [ ] Entity `VoucherUsage` + repository
- [ ] Perbarui `VoucherService` dengan validasi lengkap
- [ ] Perbarui halaman admin `/admin/vouchers`
- [ ] Daftar voucher berlaku di `home_user.html`
- [ ] Input kode voucher di `purchase_confirmation.html` + validasi AJAX
- [ ] Simpan voucher dan nominal diskon di `TransactionHistory`
- [ ] Tentukan urutan flash sale vs voucher, catat keputusannya
- [ ] Uji: voucher valid mengurangi total dan tercatat
- [ ] Uji: kedaluwarsa / kuota habis / di bawah minimum ditolak
- [ ] Uji: pelanggan sama tidak bisa pakai dua kali
- [ ] Tangkapan layar untuk BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

### SKPL-F19 Perapian tampilan tabel
Proposal: Tabel 3.5 (Services), Tabel 3.10

- [ ] `admin_sales_report.html`
- [ ] `admin_topup_report.html`
- [ ] `admin_user_management.html`
- [ ] Riwayat transaksi di `profile_user.html`
- [ ] Paginasi di sisi server dengan `Pageable`
- [ ] Pencarian, filter tanggal, urut kolom, badge status, format rupiah
- [ ] Tangkapan layar sebelum dan sesudah untuk BAB IV

Tanggal selesai: ______  Catatan: ______________________________________

---

---

## Tahap F — Pemindahan 21 halaman Thymeleaf ke React

Urutan dari paling sederhana. Controller dan template lama baru dihapus setelah
padanan React-nya terbukti jalan.

- [ ] `login`, `registration`, `forgot_password`
- [ ] `home_user` (katalog), `home`, `error`
- [ ] `profile_user`, `edit_profile`, `profile_admin`
- [ ] `purchase_confirmation`, `top_up`
- [ ] `manage_categories`, `manage_provider`, `add_product`, `update_product`
- [ ] `admin_user_management`, `admin_vouchers`, `admin_topup_report`
- [ ] `home_admin` (dashboard), `admin_sales_report` — paling akhir, bergantung F15/F16/F17
- [ ] Hapus `thymeleaf-extras-springsecurity6`, starter Thymeleaf, dan driver MySQL
- [ ] Hapus profil `supabase` dan pindahkan isinya ke `application.properties`

Tanggal selesai: ______  Catatan: ______________________________________

---

---

## Tahap G — Pengujian Black Box seluruh modul

- [ ] Buat `docs/PENGUJIAN-BLACKBOX.md`
- [ ] Skenario uji 19 modul lengkap dengan hasil
- [ ] Uji regresi seluruh fitur v1
- [ ] Kumpulkan tangkapan layar ke satu folder seperti `screenshots-bab4`
- [ ] Tandai skenario yang memakai simulasi callback Xendit, bukan pembayaran nyata

Tanggal selesai: ______  Catatan: ______________________________________

---

## Kalau ada keputusan teknis yang berubah

Setiap kali implementasi berbeda dari yang tertulis di proposal, catat di sini supaya
BAB III bisa ikut diperbarui sebelum sidang.

| Tanggal | Yang berubah | Bagian proposal yang perlu disesuaikan |
|---|---|---|
| 18 Sep 2026 | Frontend Thymeleaf diganti React + Vite | BAB I batasan poin 6 dan metodologi 1.6.2; BAB II tambah subbab React dan REST API; BAB III Tabel 3.9; kebutuhan non-fungsional NF01 dan NF06 |
| 18 Sep 2026 | Basis data MySQL/MariaDB diganti PostgreSQL (Supabase) | BAB I batasan poin 6 dan metodologi 1.6.2; BAB II 2.13 diubah dari MySQL menjadi PostgreSQL/Supabase; BAB III Tabel 3.9, XAMPP/Laragon dihapus |
| 18 Sep 2026 | Arsitektur menjadi SPA + REST API, bukan server-rendered | BAB III bagian arsitektur sistem; Daftar Pustaka perlu rujukan React, PostgreSQL, Supabase |
| 18 Sep 2026 | Beban kerja bertambah (migrasi di luar 10 fitur semula) | Tabel 1.1 Waktu Penelitian — pertimbangkan baris tambahan. **Perlu dibicarakan dengan pembimbing.** |
| 25 Sep 2026 | Database pindah ke Supabase **Projek-hanif**; data lama tidak dipindah, di-seed ulang (keputusan K1) | BAB III Tabel 3.9 |
| 25 Sep 2026 | Top up manual tidak lagi disetujui otomatis oleh scheduler; wajib diverifikasi admin | Deskripsi sistem berjalan / usulan di BAB III |
| 25 Sep 2026 | Usulan fitur baru SKPL-F20 AI chat admin (OpenRouter, menu laporan tetap, tanpa text-to-SQL) dan SKPL-F21 rating toko | Tabel 3.6, 3.10, Gambar 3.3 (aktor OpenRouter), BAB II subbab LLM & kepuasan pelanggan. **Perlu persetujuan pembimbing.** |
| 25 Sep 2026 | Satu voucher hanya boleh dipakai sekali per pelanggan (tabel `voucher_usages`) | Aturan voucher di BAB III |
| 25 Sep 2026 | Edit profil hanya nomor HP dan password; email tidak bisa diubah (dipakai untuk login Google dan OTP) | Use case kelola profil |
| 25 Sep 2026 | Ulasan toko dibuka untuk publik (`/ulasan`), semua pelanggan yang login boleh menilai (syarat ≥ 2 transaksi hanya untuk popup), admin bisa membalas ulasan | Deskripsi SKPL-F21, use case pelanggan dan admin |
| 25 Sep 2026 | Pengajuan top up manual oleh pelanggan dihapus; top up hanya lewat Xendit. Admin tetap bisa menyetujui/menolak permintaan manual lama yang masih PENDING | Use case top up pelanggan, activity diagram top up |
| 25 Sep 2026 | Produk bisa dibayar langsung lewat Xendit (tanpa saldo) dengan biaya admin 10%; jika produk gagal diproses setelah dibayar, dana masuk saldo. Profil: ubah nama pengguna dan foto profil | Use case pembelian (metode bayar), sequence diagram webhook, kamus data `product_orders`, `user_avatars` |
| 25 Sep 2026 | Kategori diganti PULSA dan PAKET DATA; katalog bertingkat kategori › provider › nominal; struk token PLN untuk printer thermal 58 mm; konfirmasi logout; username tidak membedakan huruf besar/kecil | Rancangan antarmuka katalog, use case cetak struk, aturan akun di BAB III |
| 25 Sep 2026 | Produk fisik (aksesoris) tidak dijual lewat aplikasi, hanya di konter; kategori Voucher Game (Mobile Legends, Free Fire, PUBG Mobile, Genshin Impact, Valorant) ditambahkan dengan ID game sebagai tujuan; kartu produk menampilkan logo provider | Ruang lingkup produk di BAB I/III, kamus data produk |
| 25 Sep 2026 | Pembelian pulsa/kuota meminta nomor HP tujuan, token PLN meminta nomor meter / ID pelanggan (token 20 digit); produk Token Listrik PLN 20 rb – 1 jt ditambahkan | Alur transaksi pembelian (activity/sequence diagram), atribut `customer_number` di kamus data |
| | | |
