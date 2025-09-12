# FlashCell E-commerce Website 🛒

Selamat datang di **FlashCell**, sebuah proyek aplikasi web full-stack yang menyimulasikan fungsionalitas toko online modern. Proyek ini dibangun untuk mendemonstrasikan arsitektur aplikasi web menggunakan backend **Java** dengan **Spring Boot**, dan **Thymeleaf** sebagai *template engine* untuk merender halaman secara dinamis di sisi server.

- [Tentang Proyek](#tentang-proyek)
- [Teknologi yang Digunakan](#teknologi-yang-digunakan)
- [Fitur Utama](#fitur-utama)
- [Panduan Instalasi](#panduan-instalasi)
- [Struktur Proyek](#struktur-proyek)
- [Kontribusi](#kontribusi)
- [Lisensi](#lisensi)

---

## Tentang Proyek

**FlashCell** adalah sebuah platform e-commerce fungsional yang memungkinkan pengguna untuk menjelajahi berbagai produk, menambahkannya ke keranjang belanja, dan menyelesaikan proses *checkout*. Aplikasi ini dirancang dengan arsitektur yang bersih dan mudah dipelihara, menjadikannya contoh yang baik untuk mempelajari pengembangan web dengan ekosistem Java.

Tujuan utama proyek ini adalah:
* Membangun aplikasi e-commerce yang realistis dari awal.
* Mengimplementasikan operasi **CRUD** (*Create, Read, Update, Delete*) untuk entitas seperti produk, pengguna, dan pesanan.
* Mendemonstrasikan integrasi antara backend Java dan frontend yang dirender oleh server menggunakan Thymeleaf.
* Mengelola data secara persisten menggunakan database relasional (MySQL).

---

## Teknologi yang Digunakan

Proyek ini dibangun menggunakan teknologi modern dan populer di ekosistem Java:

* **Backend**: ☕ **Java** (dengan **Spring Boot**)
    * **Spring Web**: Untuk membangun endpoint RESTful dan melayani halaman web.
    * **Spring Data JPA**: Untuk mempermudah interaksi dengan database (ORM).
    * **Spring Security**: Untuk menangani otentikasi dan otorisasi pengguna.
* **Frontend**: 🍃 **Thymeleaf**
    * *Template engine* yang terintegrasi penuh dengan Spring, memungkinkan pembuatan halaman HTML yang dinamis dengan data dari backend.
* **Database**: 🗄️ **MySQL**
    * Sistem manajemen database relasional yang andal untuk menyimpan semua data aplikasi.
* **Build Tool**: 🛠️ **Maven**
    * Untuk manajemen dependensi dan proses *build* proyek.
* **Server**: 🌐 **Embedded Tomcat**
    * Server web yang sudah termasuk di dalam Spring Boot, tidak perlu instalasi terpisah.

---

## Fitur Utama

Aplikasi ini mencakup fitur-fitur esensial dari sebuah website e-commerce:

* 👤 **Manajemen Pengguna**:
    * Registrasi dan login pengguna.
    * Sistem otentikasi berbasis sesi.
    * Profil pengguna.

* 🛍️ **Katalog Produk**:
    * Tampilan daftar produk dengan gambar dan harga.
    * Halaman detail untuk setiap produk.
    * Fitur pencarian produk.
    * Filter produk berdasarkan kategori.

* 🛒 **Keranjang Belanja**:
    * Menambah, menghapus, dan memperbarui jumlah produk di keranjang.
    * Ringkasan total belanja secara dinamis.

* 💳 **Proses Checkout**:
    * Formulir pengisian alamat pengiriman.
    * Simulasi proses pembayaran.
    * Pembuatan pesanan setelah checkout berhasil.

* 📜 **Riwayat Pesanan**:
    * Pengguna dapat melihat daftar pesanan yang pernah mereka buat.

* 🔒 **Panel Admin (Opsional)**:
    * Halaman khusus untuk admin mengelola produk (tambah, edit, hapus).
    * Melihat daftar pesanan yang masuk.
    * Mengelola data pengguna.

---

## Panduan Instalasi

Untuk menjalankan proyek ini di lingkungan lokal Anda, ikuti langkah-langkah berikut.

### Prasyarat

Pastikan perangkat Anda telah terinstal:
* **JDK 17** atau versi yang lebih baru.
* **Apache Maven** 3.6 atau versi yang lebih baru.
* **MySQL Server** 8.0 atau versi yang lebih baru.
* **Git** untuk melakukan kloning repositori.
* IDE favorit Anda (misalnya: IntelliJ IDEA, Eclipse, atau VS Code dengan ekstensi Java).

### Langkah-langkah Instalasi

1.  **Clone Repositori**
    ```bash
    git clone [https://github.com/your-username/FlashCell.git](https://github.com/your-username/FlashCell.git)
    cd FlashCell
    ```

2.  **Konfigurasi Database**
    * Buka terminal atau *client* MySQL Anda dan buat database baru.
        ```sql
        CREATE DATABASE flashcell_db;
        ```
    * Buka file `src/main/resources/application.properties`.
    * Sesuaikan konfigurasi koneksi database dengan kredensial MySQL Anda.
        ```properties
        # Konfigurasi Koneksi Database MySQL
        spring.datasource.url=jdbc:mysql://localhost:3306/flashcell_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
        spring.datasource.username=root
        spring.datasource.password=password_anda
        
        # Konfigurasi JPA & Hibernate
        spring.jpa.hibernate.ddl-auto=update
        spring.jpa.show-sql=true
        spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
        ```
        **Penting**: Ganti `password_anda` dengan password root MySQL Anda. `ddl-auto=update` akan membuat tabel secara otomatis saat aplikasi pertama kali dijalankan.

3.  **Build Proyek**
    Gunakan Maven untuk menginstal semua dependensi dan membangun proyek.
    ```bash
    mvn clean install
    ```

4.  **Jalankan Aplikasi**
    * Anda dapat menjalankan aplikasi langsung dari IDE Anda dengan menjalankan *main class* yang memiliki anotasi `@SpringBootApplication`.
    * Atau, jalankan melalui terminal dari direktori root proyek:
        ```bash
        mvn spring-boot:run
        ```

5.  **Akses Aplikasi**
    Setelah aplikasi berhasil berjalan, buka browser Anda dan akses alamat berikut:
    `http://localhost:8080`

---

## Struktur Proyek

Struktur direktori proyek ini mengikuti konvensi standar dari aplikasi Spring Boot.

```
FlashCell/
├── src/
│   ├── main/
│   │   ├── java/com/example/flashcell/  <-- Kode sumber Java (paket utama)
│   │   │   ├── controller/             <-- Mengatur HTTP request dan response
│   │   │   ├── model/                  <-- Entitas data (JPA Entities)
│   │   │   ├── repository/             <-- Interface untuk akses data (Spring Data JPA)
│   │   │   ├── service/                <-- Logika bisnis aplikasi
│   │   │   └── FlashCellApplication.java <-- Titik masuk utama aplikasi
│   │   ├── resources/
│   │   │   ├── static/                 <-- Aset statis (CSS, JavaScript, gambar)
│   │   │   ├── templates/              <-- File template Thymeleaf (.html)
│   │   │   └── application.properties  <-- File konfigurasi utama aplikasi
├── pom.xml                             <-- Konfigurasi dependensi Maven
└── README.md                           <-- Anda sedang membacanya
```

---

## Kontribusi

Kontribusi dari Anda sangat kami harapkan! Jika Anda ingin berkontribusi, silakan ikuti langkah-langkah berikut:

1.  **Fork** proyek ini.
2.  Buat *branch* fitur baru (`git checkout -b feature/FiturBaru`).
3.  *Commit* perubahan Anda (`git commit -m 'Menambahkan FiturBaru'`).
4.  *Push* ke *branch* Anda (`git push origin feature/FiturBaru`).
5.  Buka **Pull Request**.

---

## Lisensi

Proyek ini didistribusikan di bawah Lisensi MIT. Lihat file `LICENSE` untuk informasi lebih lanjut.
