package com.example.kartu.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
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
    
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    public long countTotalProducts() {
        return productRepository.count();
    }
}