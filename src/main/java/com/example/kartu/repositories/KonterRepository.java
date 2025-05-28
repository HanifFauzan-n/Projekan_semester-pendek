package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Konter;

public interface KonterRepository extends JpaRepository<Konter, Integer> {
        List<Konter> findByNamaContainingIgnoreCase(String nama);
        List<Konter> findAllByOrderByKodeKategoriAsc();
        List<Konter> findByKodeKategoriTipe(String string);
}
