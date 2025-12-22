package com.example.kartu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.kartu.dto.request.UserRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;

import jakarta.validation.Valid;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(UserRequest requestUser) throws Exception {
        // == FULL VALIDATION START ==

        // 1. Username Validation
        if (userRepository.findByUsername(requestUser.getUsername()).isPresent()) {
            throw new Exception("Username is already taken, please choose another.");
        }

        // Encrypt password
        requestUser.setPassword(passwordEncoder.encode(requestUser.getPassword()));
        // Set default role
        requestUser.setRole("ROLE_USER");
        
        userRepository.save(requestUser);
    }
}