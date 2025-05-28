package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Histori;

public interface HistoriRepository extends JpaRepository <Histori,Integer> {
    List<Histori> findByUserId(Integer integer);
    
} 
