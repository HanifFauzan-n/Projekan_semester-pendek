package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.kartu.models.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(String categoryId, String name, Pageable pageable);

    List<Product> findByCategoryType(String type);

    long countByProviderId(Integer providerId);

    Integer countByCategoryId(String categoryId);

    @Modifying(flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - 1 WHERE p.id = :id AND p.stock > 0")
    int decrementStock(@Param("id") Integer id);

    List<Product> findByStockLessThanEqualOrderByStockAsc(Integer threshold);

    long countByStockLessThanEqual(Integer threshold);
}