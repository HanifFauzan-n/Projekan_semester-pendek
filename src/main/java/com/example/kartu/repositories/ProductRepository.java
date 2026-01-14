package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByCategoryType(String type);

    long countByProviderId(Integer providerId);

    Integer countByCategoryId(String categoryId);

    // List<Product> findAllOrderByCategoryAsc();
}