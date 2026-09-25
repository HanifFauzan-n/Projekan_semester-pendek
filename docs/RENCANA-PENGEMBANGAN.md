# Rencana Pengembangan v1 → v2

Spesifikasi teknis tiap fitur. Urutan di bawah adalah urutan pengerjaan yang disepakati:
**risiko tertinggi dulu**, supaya tiga integrasi eksternal (Xendit, Google, SMTP) —
yang paling rawan macet dan paling menentukan nilai kebaruan penelitian — selesai lebih awal.

Kode SKPL mengacu ke Tabel 3.6 Analisis Kebutuhan Fungsional pada BAB III proposal.

---

## Dependency yang perlu ditambahkan ke `pom.xml`

Tambahkan bertahap, sesuai fitur yang sedang dikerjakan — jangan sekaligus di awal.

```xml
<!-- SKPL-F03 Google OAuth2 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- SKPL-F18 Notifikasi email -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-mail</artifactId>
</dependency>

<!-- SKPL-F17 Ekspor Excel -->
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>5.3.0</version>
</dependency>

<!-- SKPL-F16 Cetak PDF -->
<dependency>
  <groupId>com.github.librepdf</groupId>
  <artifactId>openpdf</artifactId>
  <version>2.0.3</version>
</dependency>
```

**SKPL-F05 Xendit tidak butuh dependency baru.** Spring Boot 3.4 sudah membawa
`RestClient` (Spring Framework 6.1+) yang cukup untuk memanggil REST API Xendit.
Jangan pakai SDK pihak ketiga — `RestClient` lebih ringan dan lebih mudah dijelaskan di laporan.

Versi library di atas silakan disesuaikan ke rilis stabil terbaru saat mulai dikerjakan;
yang penting `poi-ooxml` dan `openpdf` kompatibel dengan Java 21.

---

## Konfigurasi `application.properties`

Jangan menaruh nilai rahasia langsung di file ini. Pakai placeholder environment variable:

```properties
# --- Xendit (SKPL-F05) ---
xendit.api-key=${XENDIT_API_KEY:}
xendit.base-url=https://api.xendit.co
xendit.callback-token=${XENDIT_CALLBACK_TOKEN:}
app.base-url=${APP_BASE_URL:http://localhost:8080}

# --- Google OAuth2 (SKPL-F03) ---
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:}
spring.security.oauth2.client.registration.google.scope=openid,profile,email

# --- Email SMTP (SKPL-F18) ---
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_APP_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.mail.from=${MAIL_USERNAME:noreply@zelatancell.local}
app.mail.enabled=${MAIL_ENABLED:true}
```

Untuk pengembangan lokal, nilai aslinya disimpan di file `.env` atau di environment
variable Windows — dan `.env` masuk `.gitignore`.

---

## 1. SKPL-F05 — Top up saldo via payment gateway Xendit

**Kondisi v1:** pelanggan mengisi nominal di `/topup`, permintaan tersimpan dengan status
menunggu, admin membuka `/admin/topups` dan menyetujui satu per satu, baru saldo bertambah.

**Target v2:** pelanggan memilih nominal, sistem membuat invoice ke Xendit, pelanggan
diarahkan ke halaman pembayaran Xendit (VA / QRIS / e-wallet tersedia di sana), setelah
dibayar Xendit memanggil webhook sistem, saldo bertambah otomatis tanpa campur tangan admin.

**Pendekatan: Xendit Invoice API.** Satu endpoint membuat invoice yang sudah menyediakan
semua metode pembayaran sekaligus, dan hanya perlu satu callback. Alternatifnya memanggil
API Virtual Account, QRIS, dan e-Wallet terpisah — checkout tetap di dalam sistem sendiri
dan tampilannya lebih menyatu, tapi kodenya kira-kira tiga kali lipat dan callback-nya
ada tiga jalur. Untuk skala penelitian ini, Invoice API jauh lebih sepadan.

### Perubahan entity

`TopUp` — tambah kolom:

| Kolom | Tipe | Keterangan |
|---|---|---|
| `externalId` | String, unique | id yang dikirim ke Xendit, mis. `topup-{id}-{uuid}` |
| `xenditInvoiceId` | String | id invoice dari respons Xendit |
| `invoiceUrl` | String | URL halaman pembayaran, dipakai untuk redirect |
| `paymentMethod` | String, nullable | terisi dari callback: VA / QRIS / EWALLET |
| `paidAt` | LocalDateTime, nullable | waktu pembayaran dari callback |
| `paymentChannel` | String, nullable | mis. BCA, OVO, DANA |

Status top up: pertahankan enum yang sudah ada kalau memungkinkan, tambahkan nilai
`PENDING` → `PAID` / `EXPIRED` / `FAILED`. Cek dulu `enums/TransactionStatus.java` —
kalau enum itu khusus transaksi pembelian, buat enum baru `TopUpStatus` daripada
memaksakan satu enum untuk dua konteks.

### Kelas baru

- `services/XenditService` — bungkus panggilan HTTP ke Xendit pakai `RestClient`,
  autentikasi Basic Auth dengan API key sebagai username dan password kosong.
- `services/PaymentService` — logika bisnis: buat top up berstatus PENDING,
  minta invoice ke `XenditService`, simpan `invoiceUrl`, dan proses callback.
- `controllers/PaymentController` — endpoint pelanggan.
- `controllers/XenditWebhookController` — endpoint callback, **harus dikecualikan dari CSRF
  dan dari autentikasi** di `SecurityConfig`.

DTO `PaymentRequest`, `PaymentCallbackRequest`, `PaymentResponse` sudah ada di proyek —
periksa isinya dan sesuaikan, jangan langsung ditimpa.

### Endpoint

| Method | Path | Keterangan |
|---|---|---|
| GET | `/topup` | halaman top up (sudah ada, tambahkan pilihan bayar otomatis) |
| POST | `/topup/pay` | buat invoice Xendit, redirect ke `invoiceUrl` |
| GET | `/topup/status/{externalId}` | halaman status setelah pelanggan kembali |
| POST | `/api/xendit/callback` | webhook Xendit, publik, tanpa CSRF |

### Panggilan Xendit

Buat invoice: `POST https://api.xendit.co/v2/invoices`
Body inti: `external_id`, `amount`, `payer_email`, `description`,
`success_redirect_url`, `failure_redirect_url`, `invoice_duration`.
Autentikasi: Basic Auth, username = secret API key, password kosong.
Respons memberi `id` dan `invoice_url`.

Webhook: Xendit mengirim POST berisi `external_id`, `status` (`PAID` / `EXPIRED`),
`paid_amount`, `payment_method`, `payment_channel`, `paid_at`,
dengan header `x-callback-token`.

### Keamanan — jangan dilewati

1. **Verifikasi `x-callback-token`** dari header webhook terhadap
   `xendit.callback-token`. Tolak dengan 401 kalau tidak cocok. Tanpa ini, siapa pun
   yang tahu URL callback bisa menambah saldo sendiri.
2. **Idempoten.** Xendit bisa mengirim callback yang sama lebih dari sekali. Kalau top up
   sudah berstatus PAID, jangan tambah saldo lagi — langsung balas 200.
3. **Cocokkan nominal.** Bandingkan `paid_amount` dari callback dengan nominal top up
   yang tersimpan. Kalau beda, jangan tambah saldo, catat log dan tandai perlu ditinjau.
4. Saldo ditambah **hanya** di jalur callback, tidak pernah di `success_redirect_url` —
   URL redirect bisa dibuka manual oleh siapa saja.

### Menguji webhook di localhost

Xendit tidak bisa memanggil `localhost`. Dua pilihan:
- jalankan tunnel (ngrok atau sejenisnya), pasang URL publiknya di dashboard Xendit; atau
- panggil endpoint callback sendiri pakai `curl`/Postman dengan body tiruan —
  cukup untuk pembuktian Black Box Testing, dan catat di laporan bahwa pengujian
  dilakukan dengan simulasi callback.

### Selesai kalau

- Pelanggan bisa top up dan diarahkan ke halaman pembayaran Xendit sandbox.
- Setelah pembayaran sandbox berhasil, saldo bertambah otomatis tanpa aksi admin.
- Callback kedua dengan isi sama tidak menambah saldo dua kali.
- Callback tanpa token yang benar ditolak.
- Alur top up manual lama masih bisa dipakai sebagai cadangan.

---

## 2. SKPL-F03 — Login dengan akun Google (OAuth2)

**Kondisi v1:** hanya login username + password, dikelola `CustomUserDetailsService`
dan `SecurityConfig`.

**Target v2:** tombol "Login dengan Google" di `/login`. Login lama **tetap ada dan tetap
jadi jalur utama** — ini eksplisit di batasan masalah proposal.

### Perubahan entity

`User` — tambah kolom:

| Kolom | Tipe | Keterangan |
|---|---|---|
| `email` | String, unique | kalau belum ada; jadi kunci pencocokan akun Google |
| `authProvider` | enum `LOCAL` / `GOOGLE` | asal akun |
| `providerId` | String, nullable | `sub` dari Google |
| `profilePictureUrl` | String, nullable | foto dari Google |

`password` harus boleh null untuk akun Google. Cek dulu apakah kolom itu `nullable = false`.

### Kelas baru

- `services/CustomOAuth2UserService` extends `DefaultOAuth2UserService`
- `config/OAuth2LoginSuccessHandler` — arahkan ke `/home-admin` atau `/home-user`
  sesuai role, sama seperti handler login biasa

### Perubahan `SecurityConfig`

Tambah `.oauth2Login()` dengan `userInfoEndpoint` menunjuk ke `CustomOAuth2UserService`
dan `successHandler` ke handler baru. Jangan hapus `formLogin()` yang sudah ada.

### Aturan pencocokan akun

1. Email Google **sudah terdaftar** sebagai akun lokal → tautkan: isi `providerId`,
   set `authProvider` tetap `LOCAL` atau tambahkan penanda bahwa akun bisa dua jalur.
   Jangan bikin akun ganda dengan email sama.
2. Email **belum terdaftar** → buat akun baru, role default sama dengan registrasi biasa,
   saldo 0, `password` null.
3. Akun Google mencoba login lokal dengan password → tolak dengan pesan
   "Akun ini terdaftar melalui Google, silakan masuk dengan tombol Login dengan Google".

### Persiapan di luar kode

Buat OAuth Client ID di Google Cloud Console, tipe Web application,
Authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`.

### Selesai kalau

- Tombol Google muncul di `/login` dan berhasil memakai akun Google asli.
- Akun baru dari Google otomatis terbuat dan langsung bisa transaksi.
- Email yang sudah ada tidak menghasilkan akun ganda.
- Login username/password lama masih normal.

---

## 3. SKPL-F18 — Notifikasi email otomatis

**Kondisi v1:** tidak ada pengiriman email sama sekali.

**Target v2:** tiga pemicu sesuai proposal —
(a) registrasi akun berhasil, (b) top up saldo berhasil, (c) transaksi pembelian berhasil.

### Kelas baru

- `services/EmailService` — pakai `JavaMailSender`, kirim HTML dari template Thymeleaf.
- Template di `src/main/resources/templates/email/`:
  `welcome.html`, `topup-success.html`, `purchase-receipt.html`.
- Opsional tapi disarankan: `models/EmailLog` + repository, mencatat penerima, jenis,
  waktu, dan status kirim. Berguna sebagai bukti di BAB IV bahwa notifikasi benar terkirim.

### Aturan

1. **Pengiriman email tidak boleh menggagalkan transaksi.** Bungkus dalam try-catch,
   catat log kalau gagal, jangan lempar exception ke alur utama.
2. Kirim secara asinkron (`@Async` + `@EnableAsync`) supaya pengguna tidak menunggu SMTP.
3. Sediakan saklar `app.mail.enabled=false` untuk mematikan pengiriman saat pengujian
   fitur lain, supaya tidak membanjiri inbox.
4. Gmail butuh App Password, bukan password akun biasa (akun harus aktif 2FA).

### Isi email

- **Registrasi:** sapaan, username, tanggal daftar, tautan ke halaman login.
- **Top up:** nominal top up, metode pembayaran, saldo terbaru, waktu, id transaksi.
- **Pembelian:** nama produk, harga, voucher yang dipakai (kalau ada), total bayar,
  sisa saldo, waktu transaksi, id transaksi.

### Selesai kalau

- Ketiga pemicu benar mengirim email dan isinya sesuai data transaksi.
- SMTP mati atau salah kredensial tidak membuat registrasi/top up/pembelian gagal.

---

## 4. SKPL-F15 — Dashboard analitik dengan Chart.js

**Kondisi v1:** `/home-admin` hanya menampilkan daftar produk dan stok.

**Target v2:** kartu ringkasan + grafik, sesuai yang dijanjikan di BAB II 2.9 dan Tabel 3.10.

### Isi dashboard

Kartu ringkasan: total omset, total profit, jumlah transaksi, jumlah pelanggan aktif,
jumlah produk stok menipis.

Grafik (Chart.js, dimuat dari CDN atau disimpan di `static/js/`):
- garis — tren penjualan harian 30 hari terakhir
- batang — penjualan bulanan 12 bulan terakhir
- batang horizontal — 5 produk terlaris
- donat — komposisi penjualan per kategori atau per provider

### Kelas baru

- `services/DashboardService` — agregasi data
- `dto/response/DashboardSummaryResponse`, `ChartDataResponse`
- `controllers/DashboardController` — endpoint JSON untuk Chart.js,
  mis. `/admin/dashboard/api/sales-trend`

### Catatan

Profit dihitung dari selisih harga jual dan harga modal yang sudah ada di entity `Product`.
Pastikan harga modal tersimpan pada saat transaksi, bukan dibaca dari produk saat ini —
kalau harga modal berubah, laporan lama ikut berubah. Kalau `TransactionHistory` belum
menyimpan harga modal saat transaksi, tambahkan kolomnya.

Query agregasi sebaiknya lewat `@Query` di repository, bukan menarik semua transaksi
ke memori lalu dijumlahkan di Java.

### Selesai kalau

- Semua grafik tampil dengan data asli dari basis data, bukan data contoh.
- Angka kartu ringkasan cocok kalau dihitung manual dari tabel transaksi.

---

## 5. SKPL-F16 — Cetak laporan penjualan PDF

**Kondisi v1:** laporan di `/admin/transactions/sales` hanya tampil di layar.

**Target v2:** tombol cetak PDF dengan filter periode, hasilnya berkop toko.

### Isi PDF

Kop: nama usaha "Zelatan Cell", alamat lengkap (Jl. Temugiri 01 Tinggarjati Lor,
Desa Gentasari, Kec. Kroya, Kab. Cilacap, Jawa Tengah 53282), periode laporan,
tanggal cetak. Tabel transaksi: tanggal, id, pelanggan, produk, jumlah, harga,
diskon, total. Baris ringkasan: total transaksi, total omset, total profit.

### Kelas baru

- `services/ReportPdfService` — pakai OpenPDF
- endpoint `GET /admin/transactions/sales/pdf?start=&end=`
  dengan `Content-Disposition: attachment; filename="laporan-penjualan-{periode}.pdf"`

Alternatif tanpa library: tombol cetak browser dengan CSS `@media print`. Lebih cepat
tapi hasilnya bergantung pengaturan browser dan sulit dijamin konsisten — untuk laporan
resmi berkop, OpenPDF lebih tepat.

### Selesai kalau

- PDF terunduh, kop dan periode benar, angka cocok dengan yang tampil di layar.
- Periode tanpa transaksi menghasilkan PDF berisi keterangan data kosong, bukan error.

---

## 6. SKPL-F17 — Ekspor laporan penjualan ke Excel

**Target v2:** tombol ekspor `.xlsx` dengan filter periode yang sama dengan PDF.

### Catatan teknis

- `services/ReportExcelService` pakai Apache POI `XSSFWorkbook`.
- Header tabel dibuat tebal dengan latar warna, kolom di-`autoSizeColumn`.
- Kolom nominal diberi format angka rupiah, **bukan** teks — supaya bisa dijumlahkan
  di Excel.
- Baris terakhir memakai formula `SUM`, bukan angka hasil hitung Java, supaya terlihat
  sebagai spreadsheet yang hidup.
- Endpoint `GET /admin/transactions/sales/excel?start=&end=`,
  content type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

### Selesai kalau

- File terbuka di Excel tanpa peringatan rusak, angka bertipe numerik, total benar.

---

## 7. SKPL-F14 — Kelola flash sale dan countdown di beranda

**Kondisi v1:** tidak ada mekanisme promo berbatas waktu.

### Entity baru

`FlashSale`:

| Kolom | Tipe |
|---|---|
| `id` | Long |
| `product` | relasi ke `Product` |
| `discountType` | enum `PERCENT` / `NOMINAL` |
| `discountValue` | BigDecimal |
| `startAt` / `endAt` | LocalDateTime |
| `quota` | Integer, nullable |
| `soldCount` | Integer |
| `active` | boolean |

### Halaman

- Admin `/admin/flash-sales` — CRUD, validasi `endAt` harus setelah `startAt`,
  dan satu produk tidak boleh punya dua flash sale aktif yang periodenya bertabrakan.
- Pelanggan: bagian flash sale di `/home-user` dengan countdown timer JavaScript,
  hanya menampilkan flash sale yang sedang dalam periode aktif.

### Aturan

Harga flash sale dihitung **di sisi server** saat transaksi diproses, bukan dipercaya
dari form. Periksa ulang periode dan kuota tepat sebelum saldo dipotong — kalau periode
sudah lewat atau kuota habis, transaksi memakai harga normal dan pelanggan diberi tahu.

### Selesai kalau

- Flash sale muncul di beranda dengan hitung mundur yang benar.
- Lewat `endAt`, produk otomatis kembali ke harga normal tanpa perlu diubah admin.
- Kuota habis menghentikan diskon.

---

## 8. SKPL-F04 / SKPL-F07 / SKPL-F13 — Voucher diskon di beranda dan checkout

**Kondisi v1:** halaman `/admin/vouchers` sudah ada, entity `Voucher` sudah ada,
tapi voucher **tidak pernah tampil ke pelanggan dan tidak bisa dipakai saat membeli**.
Ini alasan status di Tabel 3.6 adalah "Pengembangan", bukan "Baru".

### Perubahan entity `Voucher`

Entity saat ini hanya 540 byte — kemungkinan besar masih sangat sederhana.
Yang perlu ada:

| Kolom | Keterangan |
|---|---|
| `code` | unique, huruf besar |
| `discountType` | `PERCENT` / `NOMINAL` |
| `discountValue` | besaran diskon |
| `minPurchase` | minimum transaksi |
| `maxDiscount` | batas atas diskon untuk tipe persen |
| `startAt` / `endAt` | masa berlaku |
| `usageLimit` / `usedCount` | batas pemakaian total |
| `active` | aktif atau tidak |

Entity baru `VoucherUsage` (voucher, user, transaksi, waktu) untuk mencegah satu
pelanggan memakai voucher yang sama berkali-kali.

### Perubahan alur

- `/home-user`: bagian daftar voucher yang sedang berlaku.
- `purchase_confirmation.html`: input kode voucher + tombol "Pakai",
  validasi via AJAX, tampilkan rincian harga sebelum diskon, diskon, dan total bayar.
- `TransactionHistory`: simpan voucher yang dipakai dan nominal diskonnya, supaya
  laporan penjualan dan perhitungan profit tetap akurat.

### Aturan

Validasi voucher **selalu diulang di server** saat transaksi diproses. Yang dicek:
aktif, dalam masa berlaku, minimum transaksi terpenuhi, kuota pemakaian belum habis,
dan pelanggan belum pernah memakainya. Diskon persen dibatasi `maxDiscount`.
Total bayar tidak boleh jadi negatif.

### Selesai kalau

- Voucher valid mengurangi total bayar dan tercatat di riwayat transaksi.
- Voucher kedaluwarsa, kuota habis, atau tidak memenuhi minimum ditolak dengan pesan jelas.
- Voucher yang sama tidak bisa dipakai dua kali oleh pelanggan yang sama.

---

## 9. SKPL-F19 — Perapian tampilan tabel

Sasaran: `admin_sales_report.html`, `admin_topup_report.html`,
`admin_user_management.html`, `profile_user.html` (riwayat transaksi).

Yang ditambahkan: kolom nomor urut, paginasi, kotak pencarian, filter rentang tanggal,
urutkan berdasarkan kolom, format rupiah yang konsisten, badge berwarna untuk status,
dan keterangan "belum ada data" yang rapi saat tabel kosong.

Paginasi sebaiknya di sisi server dengan `Pageable` Spring Data, bukan memuat
seluruh baris lalu dipotong di JavaScript.

---

## 10. Black Box Testing seluruh modul

Buat `docs/PENGUJIAN-BLACKBOX.md` berisi tabel skenario uji per modul dengan kolom:
kode uji, modul, skenario, data masukan, hasil yang diharapkan, hasil pengujian,
kesimpulan (Valid / Tidak Valid).

Sesuai BAB II 2.17, teknik yang dipakai: Equivalence Partitioning,
Boundary Value Analysis, dan Decision Table Testing.

Modul yang wajib diuji: registrasi, login username/password, login Google, katalog
dan flash sale, top up via payment gateway, transaksi pembelian, voucher diskon,
riwayat transaksi, kelola profil, kelola produk dan stok, kelola kategori dan provider,
kelola data pengguna, kelola flash sale, kelola voucher, dashboard analitik,
cetak laporan PDF, ekspor laporan Excel, notifikasi email, dan pemantauan data top up.

Ambil tangkapan layar tiap skenario sambil menguji — nanti langsung dipakai di BAB IV,
seperti yang sudah dilakukan untuk v1 di folder `screenshots-bab4`.

---

## Hal yang gampang terlewat

1. **Jangan commit kredensial.** API key Xendit, client secret Google, dan App Password
   Gmail semuanya lewat environment variable. Pastikan `.gitignore` memuat `.env`.
2. **Callback Xendit harus dikecualikan dari CSRF** di `SecurityConfig`, kalau tidak
   setiap callback akan ditolak 403 dan susah dilacak.
3. **`ddl-auto=update` tidak menghapus kolom.** Kalau ada kolom lama yang tidak terpakai
   lagi, biarkan saja atau hapus lewat SQL manual — jangan ganti ke `create-drop`,
   data pengujianmu akan hilang.
4. **Harga modal harus tersimpan di transaksi**, bukan dibaca ulang dari produk,
   supaya angka profit di dashboard dan laporan tidak berubah kalau harga produk diubah.
5. **Flash sale dan voucher bisa berlaku bersamaan** untuk satu transaksi. Tentukan
   urutannya sejak awal — disarankan: harga flash sale dulu, baru voucher diterapkan
   ke harga setelah flash sale — dan tulis keputusan itu di BAB IV.
6. **Uji ulang fitur v1 setiap selesai satu fitur baru**, terutama alur transaksi
   pembelian, karena hampir semua fitur baru menyentuhnya.
