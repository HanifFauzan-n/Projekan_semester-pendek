package com.example.kartu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Category;

public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByTypeContainingIgnoreCase(String type);

    List<Category> findAllByOrderByTypeAsc();

    Optional<Category> findById(String id);
}
