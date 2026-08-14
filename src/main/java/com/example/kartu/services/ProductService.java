package com.example.kartu.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.kartu.models.Product;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final TransactionHistoryRepository transactionHistoryRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    public void deleteById(Integer id) throws Exception {
        boolean hasHistory = transactionHistoryRepository.findAll()
                .stream().anyMatch(h -> h.getProduct().getId().equals(id));

        if (hasHistory) {
            throw new Exception("Product cannot be deleted because it has transaction history.");
        }
        productRepository.deleteById(id);
    }

    public Page<Product> getAllProductsPaged(int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return productRepository.findAll(pageable);
    }

    public Page<Product> searchByNamePaged(String keyword, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    public long countTotalProducts() {
        return productRepository.count();
    }

    // Metode untuk menangani Filter + Pencarian + Paginasi
    public Page<Product> getProductsWithFilter(String keyword, String categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // A. Jika ada Penyaring Kategori
        if (categoryId != null && !categoryId.isEmpty()) {
            if (keyword != null && !keyword.isEmpty()) {
                // Pencarian + Kategori
                return productRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, keyword, pageable);
            } else {
                // Hanya Kategori
                return productRepository.findByCategoryId(categoryId, pageable);
            }
        }

        // B. Jika TIDAK ada Penyaring Kategori (Hanya Pencarian atau Semua)
        else {
            if (keyword != null && !keyword.isEmpty()) {
                // Hanya Pencarian
                return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
            } else {
                // Tampilkan Semua
                return productRepository.findAll(pageable);
            }
        }
    }
}
