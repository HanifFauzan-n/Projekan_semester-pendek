# Zelatan Cell — Sistem Informasi Penjualan & Manajemen Persediaan

Konteks untuk sesi Claude Code. Baca file ini dulu sebelum mengubah apa pun.

## Apa proyek ini

Sistem informasi penjualan konter pulsa Zelatan Cell (Desa Gentasari, Kroya, Cilacap).
Versi pertama (v1) sudah jadi dan dipakai. Pekerjaan sekarang adalah **pengembangan v1 → v2**,
bukan membangun dari nol. Landasan akademis: Proposal UP "Pengembangan Sistem Informasi Penjualan
dan Manajemen Persediaan Berbasis Web Menggunakan Metode Waterfall pada Usaha Zelatan Cell"
(Hanif Fauzan Nurrahman, NIM 02032311025, S1 Teknik Informatika, Universitas Nasional Pasim).

Dokumen:
- `../plan.md` — rencana lanjutan (fase 0–5, keputusan K1–K9), disusun 25 Sep 2026
- `docs/CHECKLIST-PROGRES.md` — checklist per fitur + hasil uji untuk BAB IV
- `docs/RENCANA-PENGEMBANGAN.md`, `docs/KONTEKS-SESI.md` — spesifikasi dan keputusan awal (sebagian sudah usang, lihat status di bawah)
- `docs/sql/` — skrip SQL yang sudah dijalankan di database, berurutan

## Status (25 September 2026)

Arsitektur sudah final: **React (Vite) → REST API Spring Boot → PostgreSQL Supabase (Projek-hanif)**.
Thymeleaf dan driver MySQL sudah dihapus.

| Fitur | Status |
|---|---|
| F05 top up Xendit (Invoice API, sandbox) | jalan; webhook butuh ngrok ke port 8080 (URL terdaftar di dashboard Xendit) |
| F03 login Google | jalan; logika akun di `GoogleAccountService`, akun BANNED ditolak |
| F18 email | OTP, selamat datang, top up berhasil, struk pembelian (dikirim setelah commit) |
| F15 dashboard | tren 30 hari + bulanan 12 bulan, agregasi JPQL di `ReportService` |
| F16 / F17 | PDF berkop (OpenPDF) dan `.xlsx` dengan rumus SUM (Apache POI), `SalesReportService` |
| F14 flash sale | jalan; periode bertabrakan ditolak, kuota diklaim atomik |
| F04/F07/F13 voucher | jalan; satu voucher sekali per pelanggan (`voucher_usages`) |
| F19 tabel | laporan, top up, pengguna, rating dipaginasi dan dicari di server |
| F20 (usulan) AI chat admin | OpenRouter model gratis, menu laporan tetap (tanpa text-to-SQL); butuh `OPENROUTER_API_KEY` |
| F21 (usulan) rating "Nilai Kami" | ulasan publik di `/ulasan` (tanpa login); semua pelanggan yang login boleh menilai; popup otomatis hanya setelah ≥ 2 transaksi sukses; admin membalas di `/admin/ratings` (balasan terhapus kalau pelanggan mengubah ulasan) |

F20 dan F21 **belum ada di Tabel 3.6** proposal — perlu persetujuan pembimbing.

## Aturan penting

1. **Jangan merusak fitur v1.** Uji ulang alur pembelian dan top up setiap selesai mengubah sesuatu.
2. **Login username/password wajib tetap ada.** Google hanya alternatif (batasan masalah proposal).
3. **Xendit selalu sandbox.** Kredensial hanya di `.env` (tidak di-commit); `application.properties`
   hanya berisi placeholder `${...}`.
4. **Setiap fitur selesai = ada skenario Black Box.** Hasilnya dipakai untuk BAB IV.
5. **Kode fitur mengikuti SKPL-F01..F19** (Tabel 3.6), ditambah F20/F21 usulan. Sebut di commit,
   misal `feat(SKPL-F05): ...`.
6. Bahasa kode dan komentar: Inggris. Teks untuk pengguna (label, pesan error, email): Bahasa Indonesia.
7. **Uang dan kuota selalu diubah dengan UPDATE bersyarat yang atomik**
   (`UserRepository.debitBalance/creditBalance`, `ProductRepository.decrementStock`,
   `VoucherRepository.claimUsage`, `FlashSaleRepository.claimSlot`,
   `TopUpRepository.resolvePendingManual`). Jangan baca-ubah-simpan saldo lewat entity.
   `User` memakai `@DynamicUpdate` supaya menyimpan entity tidak menimpa saldo.
8. **Satu origin.** Saat dev, Vite proxy `/api`, `/oauth2`, `/login/oauth2` ke 8080; saat produksi
   hasil build React disalin ke `src/main/resources/static/`. `SpaFallbackConfig` menyerahkan semua
   alamat halaman ke `index.html` (React Router, termasuk halaman 404); rute React baru tidak perlu
   didaftarkan di backend. Halaman admin dijaga `AdminRoute` (non-admin → `/akses-ditolak`).
9. **CSRF aktif** untuk semua `/api/**` kecuali `/api/xendit/**` (webhook, diverifikasi X-CALLBACK-TOKEN).
   Frontend mengirim header `X-XSRF-TOKEN` otomatis (`frontend/src/api/client.js`).
10. **API tidak mengembalikan entity mentah yang berisi data sensitif** (password, OTP). Petakan ke Map/DTO.
11. Query agregasi ditulis JPQL, bukan SQL native. Setiap query = satu round-trip ke Supabase:
    endpoint baca diberi `@Transactional(readOnly = true)` (satu transaksi per permintaan) dan
    `hibernate.default_batch_fetch_size` aktif untuk mencegah N+1.
12. **Skema:** tabel lama dikelola `ddl-auto=update`; tabel baru dibuat lewat file di `docs/sql/`
    (constraint + RLS), lalu dijalankan di Supabase. Semua tabel `public` wajib RLS aktif
    (tanpa policy; Data API tidak dipakai, backend tersambung sebagai pemilik tabel).
13. **Username tidak case-sensitive.** Cari akun selalu lewat `UserRepository.findByUsername`
    (sudah `LOWER(...) = LOWER(...)`, didukung unique index `lower(username)`); jangan tambah query
    username lain yang membandingkan huruf apa adanya.
14. AI chat tidak boleh menulis SQL. Tambah kemampuan baru dengan menambah laporan di `ReportService.MENU`.

## Stack

| | |
|---|---|
| Backend | Java 21, Spring Boot 3.4.6 (Web, Data JPA, Security, Validation, OAuth2 Client, Mail), Lombok, OpenPDF 2.4, Apache POI 5.5 |
| Frontend | React 19, Vite 8, Tailwind 4, Chart.js, axios (`../frontend`) |
| Database | PostgreSQL 17, Supabase **Projek-hanif** (ref `mevzhxisbubhlnvwamje`, ap-southeast-1) |
| Integrasi | Xendit Invoice API, Google OAuth2/OIDC, SMTP Gmail, OpenRouter |

Koneksi database: Session pooler `aws-0-ap-southeast-1.pooler.supabase.com:5432`, user aplikasi
`zelatan_app.mevzhxisbubhlnvwamje` (role khusus, pemilik tabel aplikasi; bukan `postgres`).

## Struktur

```
src/main/java/com/example/kartu/
  api/            REST controller (Auth, Product, Promotion, Transaction, TopUp, Rating, User,
                  AdminDashboard, AdminManagement, AiChat, XenditWebhook)
  config/         SecurityConfig, SpaCsrfTokenRequestHandler, SpaFallbackConfig, OAuth2LoginSuccessHandler, XenditProperties
  services/       Auth, User, GoogleAccount, CustomOAuth2User, CustomOidcUser, CustomUserDetails,
                  TopUp, Payment, Xendit, TransactionHistory, Rating, Report, SalesReport,
                  AiChat, OpenRouterClient, Email
  models/         User, Product, Category, Provider, TopUp, TransactionHistory, Voucher, VoucherUsage,
                  FlashSale, StoreRating, AiChatSession, AiChatMessage
  repositories/   satu per model
  dto/            LoginRequest, UserRequest, UserProfileRequest, Xendit*
  seed/           DataSeed (admin + kategori), DataDummy (data contoh, idempoten)
src/main/resources/
  application.properties   hanya placeholder ${...}
  static/                  hasil build React + img/ (logo provider untuk seeder)
docs/sql/                  001 RLS+index FK, 002 store_ratings, 003 ai_chat, 004 voucher_usages, 005 balasan ulasan,
                           006 produk token PLN, 007 produk voucher game, 008 pulsa & paket data (data),
                           009 unique index lower(username), 010 pesanan Xendit, 011 foto profil
```

Nomor tujuan pembelian: `enums/PurchaseTarget` menentukan dari nama kategori apakah produk butuh
nomor HP (PULSA/KUOTA/DATA/CREDIT), nomor meter PLN (PLN/LISTRIK, token 20 digit) atau ID game (GAME).
Produk tanpa target = barang fisik: tidak tampil di `/api/products` dan ditolak saat dibeli (hanya di konter).
Logo provider: `static/img/<nama provider>.png`, dimuat ulang ke tabel `provider` oleh `DataDummy` setiap
start, disajikan `GET /api/providers/{id}/logo`. Logo PLN dan game adalah placeholder buatan sendiri;
ganti file PNG-nya dengan logo resmi lalu restart.
Beranda: katalog bertingkat kategori › provider › nominal (kategori dengan satu provider langsung ke nominal).
Struk: `/struk/:transactionId` (printer thermal 58 mm, gaya `.receipt` di `index.css`), data dari
`GET /api/transactions/{id}/receipt` yang hanya boleh dibaca pemilik transaksi atau admin.
Bayar produk via Xendit: `OrderService` (external id `order-...`, webhook yang sama dengan top up).
Tagihan = harga + biaya admin 10%. Saat PAID: pesanan PENDING→PAID (atomik) dan harga dikreditkan ke
saldo, lalu `purchaseProduct(..., "XENDIT", fee, maxCharge)` yang biasa. Gagal diproses → seluruh
tagihan tetap di saldo. `amount_paid` di laporan tidak termasuk biaya admin (`admin_fee` terpisah).
Foto profil: `PUT/DELETE /api/user/avatar`, disimpan di `user_avatars` sebagai JPEG 256 px hasil encode ulang.

## Menjalankan

1. Salin `.env.example` → `.env`, isi kredensial (database, admin, Xendit, Google, SMTP, OpenRouter).
2. Backend: `mvn spring-boot:run` di folder ini → http://localhost:8080
3. Frontend (dev): `npm run dev` di `../frontend` → http://localhost:5173
4. Webhook Xendit: `ngrok http 8080`, URL publiknya harus sama dengan yang terdaftar di dashboard
   Xendit (Settings → Webhooks → Invoice paid), dan `XENDIT_CALLBACK_TOKEN` sama dengan verification token.
5. Build produksi: `npm run build` di `../frontend`, salin isi `dist/` ke `src/main/resources/static/`
   (pertahankan `static/img/`).

Admin awal di-seed dari `ADMIN_USERNAME` / `ADMIN_PASSWORD` di `.env`.
Akun dummy (`rizky21`, `salsa08`, ... password `password123`) memakai domain `@zelatancell.test`;
`EmailService` tidak mengirim ke domain `.test`/`.local` (hanya log).

## Pengujian

- Unit test (tanpa database): `mvn test -Dtest='*ServiceTest,*CallbackTest,*DecisionTest,AuthServiceOtpTest,PurchaseTargetTest'`
- `KartuApplicationTests` memakai database dari `.env` dan menjalankan semua laporan menu AI (hanya membaca).
