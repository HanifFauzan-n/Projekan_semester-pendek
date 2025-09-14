package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Category;

public interface CategoryRepository extends JpaRepository <Category,Integer> {
    List<Category> findByTipeContainingIgnoreCase(String tipe);
    List<Category> findAllByOrderByTipeAsc();
    Category findByKode(Integer kode);
}
