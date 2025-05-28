package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Kategori;

public interface KategoriRepository extends JpaRepository <Kategori,Integer> {
    List<Kategori> findByTipeContainingIgnoreCase(String tipe);
    List<Kategori> findAllByOrderByTipeAsc();
    Kategori findByKode(Integer kode);
}
