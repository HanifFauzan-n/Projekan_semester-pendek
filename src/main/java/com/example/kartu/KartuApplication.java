package com.example.kartu;

import org.springframework.boot.CommandLineRunner; // Ganti import ke Category
import org.springframework.boot.SpringApplication; // Ganti import ke CategoryRepository
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.kartu.models.Category;
import com.example.kartu.repositories.CategoryRepository;

@SpringBootApplication
public class KartuApplication {

	public static void main(String[] args) {
		SpringApplication.run(KartuApplication.class, args);
	}

	// TAMBAHKAN BEAN INI
	// Bean ini akan dijalankan secara otomatis saat aplikasi dimulai
	@Bean
        @SuppressWarnings("unused")
	CommandLineRunner initDatabase(CategoryRepository categoryRepository) {
		return args -> {
			// Cek jika tabel kategori kosong
			if (categoryRepository.count() == 0) {
				System.out.println("Initializing categories...");
				// Simpan kategori default (dalam Bahasa Inggris dan huruf besar untuk konsistensi)
				categoryRepository.save(new Category(1, "PULSE")); // sebelumnya "pulsa"
				categoryRepository.save(new Category(2, "DATA"));  // sebelumnya "kuota"
				System.out.println("Categories initialized.");
			}
		};
	}
}