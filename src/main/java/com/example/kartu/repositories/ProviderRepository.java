package com.example.kartu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.Provider;

public interface ProviderRepository extends JpaRepository<Provider,Integer> {
    
}
