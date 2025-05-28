package com.example.kartu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.User;

public interface UserRepository extends JpaRepository <User,Integer>{
    
}
