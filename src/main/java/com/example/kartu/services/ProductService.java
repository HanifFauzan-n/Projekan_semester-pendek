package com.example.kartu.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.kartu.models.Product;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    public void deleteById(Integer id) throws Exception {
        boolean hasHistory = transactionHistoryRepository.findAll()
                .stream().anyMatch(h -> h.getProduct().getId().equals(id));

        if (hasHistory) {
            throw new Exception("Cannot delete product because it has a transaction history.");
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

    // Method Sakti untuk Menangani Filter + Search + Pagination
    public Page<Product> getProductsWithFilter(String keyword, String categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // A. Jika ada Filter Kategori
        if (categoryId != null && !categoryId.isEmpty()) {
            if (keyword != null && !keyword.isEmpty()) {
                // Search + Kategori
                return productRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, keyword, pageable);
            } else {
                // Cuma Kategori
                return productRepository.findByCategoryId(categoryId, pageable);
            }
        }

        // B. Jika TIDAK ada Filter Kategori (Cuma Search atau All)
        else {
            if (keyword != null && !keyword.isEmpty()) {
                // Cuma Search
                return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
            } else {
                // Tampilkan Semua
                return productRepository.findAll(pageable);
            }
        }
    }
}