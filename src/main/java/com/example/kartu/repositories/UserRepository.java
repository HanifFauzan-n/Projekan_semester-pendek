package com.example.kartu.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.User;

public interface UserRepository extends JpaRepository <User,Integer>{
    Optional<User> findByUsername(String username);
}
