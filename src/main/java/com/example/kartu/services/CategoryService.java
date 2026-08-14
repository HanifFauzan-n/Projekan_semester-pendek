package com.example.kartu.services;

import com.example.kartu.models.Category;
import com.example.kartu.repositories.CategoryRepository;
import com.example.kartu.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final ProductRepository productRepository;

    // 1. Ambil Semua Data
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    // 2. Simpan Kategori (Logika Huruf Besar dipindah ke sini)
    public void saveCategory(Category category) {

        // Ubah jadi huruf besar sebelum simpan
        if (category.getType() != null) {
            category.setType(category.getType().toUpperCase());
        }
        categoryRepository.save(category);
    }

    // 3. Hapus Kategori (Dengan Validasi)
    public void deleteCategory(String id) throws Exception {
        // Cek dulu apakah kategori ini dipakai oleh produk?
        Integer count = productRepository.countByCategoryId(id);

        if (count > 0) {
            throw new Exception("Operation failed. Category is active in " + count + " products.");
        }

        categoryRepository.deleteById(id);
    }
}
