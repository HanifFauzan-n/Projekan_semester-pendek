package com.example.kartu.services;

import com.example.kartu.models.Category;
import com.example.kartu.repositories.CategoryRepository;
import com.example.kartu.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository; // Inject untuk validasi

    // 1. Ambil Semua Data
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    // 2. Simpan Kategori (Logic Huruf Besar dipindah ke sini)
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
            throw new Exception("Gagal! Kategori ini sedang digunakan oleh " + count + " produk.");
        }

        categoryRepository.deleteById(id);
    }
}