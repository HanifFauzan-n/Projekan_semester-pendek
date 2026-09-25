# Migrasi: Thymeleaf → React, MySQL → PostgreSQL (Supabase)

Ditetapkan 18 September 2026.

## Arsitektur yang dipilih

```
React (Vite)  ──HTTP──►  Spring Boot REST API  ──JDBC──►  Supabase PostgreSQL
                              │
                              ├──► Xendit (payment gateway)
                              ├──► Google OAuth 2.0
                              └──► SMTP (notifikasi email)
```

**Supabase dipakai HANYA sebagai PostgreSQL terkelola.** Bukan PostgREST,
bukan Supabase Auth, bukan Row Level Security. Spring Boot tetap satu-satunya
pintu ke basis data.

Konsekuensi yang menguntungkan:

- Seluruh kode Java tetap terpakai, termasuk `XenditService` yang sudah dibuat.
- Google OAuth2, notifikasi email, laporan PDF/Excel tetap ditangani backend.
- **RLS tidak perlu dipikirkan sama sekali.** React tidak pernah menyentuh
  Supabase langsung, jadi tidak ada kunci anon yang beredar di browser dan
  tidak ada kebijakan baris yang perlu disusun.
- Proposal tidak perlu dirombak: cukup ganti sebutan MySQL menjadi
  PostgreSQL/Supabase dan Thymeleaf menjadi React.

## Cara migrasi: bertahap, berdampingan

Thymeleaf **tidak** dibuang sekaligus. Spring Boot ditambah lapisan REST API,
React dibangun di sebelahnya, halaman dipindah satu per satu. Di setiap titik
waktu selalu ada versi yang bisa didemokan ke pembimbing.

Profil Spring dipakai untuk memisahkan basis data:

- profil default (`application.properties`) → MySQL lokal, sistem lama
- profil `supabase` (`application-supabase.properties`) → PostgreSQL Supabase

```bash
./mvnw spring-boot:run                                      # MySQL, seperti biasa
./mvnw spring-boot:run -Dspring-boot.run.profiles=supabase   # Supabase
```

---

## Urutan pengerjaan yang disarankan

**Database dulu, sebelum menambah fitur lagi.** Alasannya: setiap fitur yang
dibangun di atas MySQL harus diuji ulang setelah pindah ke PostgreSQL. Kalau
migrasi database dikerjakan lebih dulu, SKPL-F05 sampai F19 cukup diuji sekali.

| Tahap | Isi |
|---|---|
| **A** | Migrasi database ke Supabase (perkiraan 1–2 hari) |
| **B** | Integrasi eksternal: SKPL-F05 Xendit, F03 Google OAuth2, F18 email — kini langsung di atas PostgreSQL |
| **C** | Lapisan REST API dan kerangka React, berjalan paralel dengan B ke atas |
| **D** | Penyajian informasi: SKPL-F15 dashboard, F16 PDF, F17 Excel — halaman barunya langsung dibuat di React |
| **E** | Promosi dan tampilan: SKPL-F14 flash sale, voucher, F19 tabel |
| **F** | Pemindahan 21 halaman Thymeleaf lama ke React, Thymeleaf dipensiunkan |
| **G** | Pengujian Black Box seluruh modul |

Penomoran ini sama dengan yang dipakai `docs/CHECKLIST-PROGRES.md`.

---

## Tahap A — Migrasi database

### A.1 Yang sudah disiapkan

- `pom.xml` — driver `org.postgresql:postgresql` ditambahkan, versi dikelola
  Spring Boot parent. Driver MySQL sengaja dibiarkan sampai migrasi selesai.
- `src/main/resources/application-supabase.properties` — profil Supabase.
- `.env.example` — variabel `SUPABASE_DB_HOST`, `SUPABASE_DB_USER`,
  `SUPABASE_DB_PASSWORD`.

### A.2 Koneksi — jangan salah pilih

Ambil dari Supabase Dashboard → **Connect** → tab **Session pooler**.

| Mode | Port | IPv4 | Cocok untuk Spring Boot |
|---|---|---|---|
| Direct (`db.[ref].supabase.co`) | 5432 | hanya dengan add-on berbayar | tidak, pada plan gratis |
| **Session pooler** (`aws-0-[region].pooler.supabase.com`) | **5432** | **semua plan** | **ya, ini yang dipakai** |
| Transaction pooler | 6543 | semua plan | tidak, prepared statement tidak didukung |

Perhatikan username Session pooler berbentuk `postgres.[PROJECT-REF]`,
bukan `postgres` saja. Salah di bagian ini menghasilkan error autentikasi
yang membingungkan.

### A.3 Jebakan PostgreSQL yang harus diperiksa SEBELUM menjalankan aplikasi

Ini bukan daftar teoretis — semuanya menyebabkan aplikasi gagal jalan.

**1. Tabel bernama `user` akan menolak jalan.** `user` adalah kata kunci di
PostgreSQL, dan `SELECT * FROM user` bukan query yang sah di sana — di MySQL
sah. Entity `models/User.java` hampir pasti memetakan ke tabel `user`.
Perbaikannya: beri anotasi eksplisit

```java
@Entity
@Table(name = "users")
public class User { ... }
```

Kalau tabel MySQL lama bernama `user`, ganti namanya saat memindahkan data.
Periksa juga nama tabel lain yang bertabrakan dengan kata kunci PostgreSQL:
`order`, `group`, `session`, `transaction`, `default`, `check`.
Entity `TransactionHistory` perlu dicek petanya.

**2. Strategi ID berubah perilaku.** `GenerationType.AUTO` di MySQL memakai
AUTO_INCREMENT, di PostgreSQL memakai sequence. Kalau data lama dipindahkan
beserta nilai id-nya, sequence harus dimajukan, kalau tidak insert berikutnya
menabrak id yang sudah ada:

```sql
SELECT setval(pg_get_serial_sequence('users','id'), (SELECT MAX(id) FROM users));
```

Ulangi untuk setiap tabel. Lebih aman: pakai `GenerationType.IDENTITY` secara
eksplisit di semua entity, perilakunya konsisten di kedua basis data.

**3. Query native bergaya MySQL akan pecah.** Cari `nativeQuery = true` di
seluruh repository. Yang khas MySQL dan tidak ada di PostgreSQL:
`LIMIT x, y` (PostgreSQL memakai `LIMIT y OFFSET x`), `IFNULL` (pakai `COALESCE`),
`DATE_FORMAT` (pakai `TO_CHAR`), `NOW()` masih jalan, `ON DUPLICATE KEY UPDATE`
(pakai `ON CONFLICT`), dan backtick `` ` `` (pakai tanda kutip ganda).

Ini penting untuk SKPL-F15: query agregasi dashboard sebaiknya ditulis dengan
JPQL atau Criteria API, bukan SQL native, supaya tidak terikat satu basis data.

**4. Boolean berbeda tipe.** MySQL menyimpan boolean sebagai `tinyint(1)`,
PostgreSQL punya tipe `boolean` sungguhan. Kolom `active` pada `Voucher` dan
`FlashSale` perlu diperiksa saat memindahkan data: nilai `0`/`1` harus menjadi
`false`/`true`.

**5. Dialect Hibernate harus ikut diganti.** Profil default memasang
`org.hibernate.dialect.MySQLDialect` secara eksplisit. Profil `supabase` sudah
memasang `PostgreSQLDialect`, tapi pastikan nilai dari profil default tidak
ikut terbawa — kalau terbawa, Hibernate akan membuat SQL bergaya MySQL untuk
PostgreSQL dan gagal dengan pesan yang menyesatkan.

**6. Nama kolom peka huruf besar-kecil.** PostgreSQL melipat nama tak berkutip
menjadi huruf kecil. Strategi penamaan bawaan Hibernate sudah mengubah
`externalId` menjadi `external_id`, jadi aman — asalkan tidak ada
`@Column(name = "externalID")` dengan huruf besar di tengah.

### A.4 Langkah pemindahan

1. Buat proyek Supabase, pilih region terdekat (Singapore / `ap-southeast-1`).
2. Simpan password database yang muncul saat pembuatan proyek — hanya
   ditampilkan sekali.
3. Isi `SUPABASE_DB_*` di `.env`.
4. Perbaiki lebih dulu semua temuan A.3 pada entity.
5. Jalankan dengan profil supabase dan `ddl-auto=update` agar Hibernate
   membentuk skema kosong di Supabase.
6. Periksa hasilnya di Supabase Table Editor: apakah semua tabel dan kolom
   terbentuk sebagaimana mestinya.
7. Pindahkan data. Untuk ukuran data penelitian ini, cara paling sederhana:
   ekspor tiap tabel MySQL menjadi CSV, lalu impor lewat Supabase Table Editor
   atau `\copy` di SQL Editor. Alternatif otomatis: `pgloader`.
8. Majukan semua sequence (lihat A.2 poin 2).
9. Ubah `ddl-auto` menjadi `validate` pada profil supabase.
10. Uji regresi seluruh fitur v1 dengan profil supabase.

### A.5 Selesai kalau

- Semua fitur v1 berjalan normal dengan profil `supabase`.
- Data lama utuh: jumlah baris tiap tabel sama dengan di MySQL.
- Insert baru tidak menabrak id yang sudah ada.
- Aplikasi masih bisa dijalankan dengan profil default (MySQL) sebagai cadangan.

---

## Tahap C — REST API dan kerangka React

### C.1 Keputusan yang menghindarkan banyak masalah

**Frontend dan backend dibuat satu origin.** Ini menghilangkan seluruh
kategori masalah CORS, cookie `SameSite`, dan CSRF lintas domain sekaligus.

Saat pengembangan, Vite memakai proxy:

```js
// frontend/vite.config.js
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api':   { target: 'http://localhost:8080', changeOrigin: true },
      '/login': { target: 'http://localhost:8080', changeOrigin: true },
      '/oauth2':{ target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
```

Saat produksi, hasil build React disalin ke `static/` milik Spring Boot dan
dilayani dari domain yang sama.

Karena satu origin, **sesi login Spring Security yang sudah ada tetap dipakai
apa adanya** — tidak perlu JWT, tidak perlu menulis ulang autentikasi, dan
yang terpenting: **alur redirect Google OAuth2 standar tetap berfungsi**.
Kalau React dijadikan origin terpisah, SKPL-F03 harus ditulis dengan pola
SPA yang jauh lebih rumit. Ini alasan utama memilih satu origin.

Tulis CSRF token ke cookie yang bisa dibaca JavaScript, lalu kirimkan kembali
lewat header `X-XSRF-TOKEN` pada setiap permintaan yang mengubah data:

```java
http.csrf(csrf -> csrf.csrfTokenRepository(
        CookieCsrfTokenRepository.withHttpOnlyFalse()));
```

Endpoint webhook Xendit tetap dikecualikan dari CSRF seperti rencana semula.

### C.2 Susunan folder

```
Projekan_semester-pendek/
  src/main/java/...        backend Spring Boot (tetap)
  src/main/resources/
    templates/             Thymeleaf lama, menyusut seiring migrasi
    static/                nanti berisi hasil build React
  frontend/                proyek React baru
    src/
      api/                 pembungkus fetch
      components/
      pages/
      hooks/
    vite.config.js
    package.json
```

### C.3 Lapisan REST API

Setiap controller Thymeleaf yang dipindah mendapat pasangan REST-nya di bawah
prefix `/api`. Controller lama **tidak dihapus** sampai halaman React-nya
terbukti jalan.

| Thymeleaf lama | REST baru |
|---|---|
| `GET /home-user` | `GET /api/products` |
| `GET /categories` | `GET /api/categories` |
| `POST /transaction/confirm/{id}` | `POST /api/transactions` |
| `GET /profile-user` | `GET /api/me`, `GET /api/me/transactions` |
| `GET /topup` | `GET /api/topups`, `POST /api/topups/pay` |
| `GET /admin/users` | `GET /api/admin/users` |
| `GET /admin/transactions/sales` | `GET /api/admin/reports/sales` |

Balikan API memakai DTO response yang sudah ada di `dto/response`, jangan
mengembalikan entity JPA langsung — relasi lazy akan memicu error serialisasi
dan membocorkan kolom yang tidak perlu (misalnya hash password).

### C.4 Pustaka frontend

Secukupnya saja, jangan berlebihan:

- `react`, `react-dom`, `react-router-dom`
- `vite`, `@vitejs/plugin-react`
- `chart.js` + `react-chartjs-2` — langsung menjawab SKPL-F15
- pengambilan data: `fetch` bawaan sudah cukup; kalau ingin lebih rapi,
  `@tanstack/react-query`
- CSS: pilih satu dan konsisten. Tailwind paling cepat, tapi CSS biasa juga
  sah — 8 berkas CSS yang sudah ada bisa dipindahkan.

### C.5 Integrasi build (kerjakan paling akhir)

Setelah React siap, tambahkan `frontend-maven-plugin` ke `pom.xml` supaya
`./mvnw package` sekalian membangun React dan menyalin hasilnya ke `static/`,
sehingga penyerahan cukup satu berkas jar. Jangan dikerjakan di awal —
saat pengembangan, jalankan Vite dan Spring Boot terpisah.

---

## Tahap F — Pemindahan 21 halaman

Urutan yang disarankan, dari yang paling sederhana ke paling rumit, supaya
pola kerjanya terbentuk lebih dulu di halaman yang risikonya kecil:

1. `login`, `registration`, `forgot_password`
2. `home_user` (katalog), `home`, `error`
3. `profile_user`, `edit_profile`, `profile_admin`
4. `purchase_confirmation`, `top_up`
5. `manage_categories`, `manage_provider`, `add_product`, `update_product`
6. `admin_user_management`, `admin_vouchers`, `admin_topup_report`
7. `home_admin` (dashboard), `admin_sales_report` — paling akhir karena
   keduanya bergantung pada SKPL-F15, F16, dan F17

`fragments/navbar.html` menjadi komponen layout React.

---

## Dampak ke proposal — dicatat, belum dikerjakan

Keputusan: **kode dulu, proposal disusulkan** setelah arsitektur final.
Yang nanti perlu diubah, supaya tidak ada yang terlewat:

| Bagian | Isi sekarang | Perlu menjadi |
|---|---|---|
| BAB I Batasan Masalah poin 6 | "Java dengan Spring Framework, basis data MySQL/MariaDB" | tambahkan PostgreSQL (Supabase) dan React |
| BAB I Metodologi 1.6.2 tahap Pengkodean | "HTML/CSS/JavaScript/Thymeleaf untuk antarmuka, serta MariaDB/MySQL" | React untuk antarmuka, PostgreSQL untuk basis data |
| BAB I Abstrak (ID dan EN) | menyebut sistem berbasis web tanpa rincian stack | sesuaikan kalau stack ikut disebut |
| BAB II 2.13 | subbab "MySQL / MariaDB" | ubah menjadi PostgreSQL dan Supabase |
| BAB II | belum ada subbab React maupun REST API | tambahkan subbab React dan arsitektur REST API |
| BAB III Tabel 3.9 | Thymeleaf, MariaDB/MySQL, XAMPP/Laragon | React + Vite, PostgreSQL (Supabase), hapus XAMPP kalau tidak lagi dipakai |
| BAB III Kebutuhan Non-Fungsional | SKPL-NF01 dan NF06 soal peramban | sesuaikan dengan sifat aplikasi satu halaman (SPA) |
| Daftar Pustaka | belum ada rujukan React, PostgreSQL, Supabase | tambahkan dokumentasi resmi ketiganya |
| Tabel 1.1 Waktu Penelitian | 5 kegiatan, Sep 2026 – Jan 2027 | pertimbangkan tambahan baris untuk migrasi |

**Perlu dibicarakan dengan pembimbing.** Mengganti stack setelah proposal
disetujui umumnya perlu sepengetahuan pembimbing, dan penambahan beban kerja
ini menyentuh jadwal di Tabel 1.1.
