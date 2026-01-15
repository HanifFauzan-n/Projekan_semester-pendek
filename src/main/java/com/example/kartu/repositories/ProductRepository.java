package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(String categoryId, String name, Pageable pageable);

    List<Product> findByCategoryType(String type);

    long countByProviderId(Integer providerId);

    Integer countByCategoryId(String categoryId);

    // List<Product> findAllOrderByCategoryAsc();
}