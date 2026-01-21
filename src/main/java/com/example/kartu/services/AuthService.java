package com.example.kartu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.kartu.dto.request.UserRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;


@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(UserRequest requestUser) throws Exception {

        // 1. Username Validation
        if (userRepository.findByUsername(requestUser.getUsername()).isPresent()) {
            throw new Exception("Username is already taken, please choose another.");
        }

        User user = new User();
        user.setUsername(requestUser.getUsername());
        user.setPassword(passwordEncoder.encode(requestUser.getPassword())); // Enkripsi password
        user.setPhoneNumber(requestUser.getPhoneNumber());
        user.setDanaNumber(requestUser.getDanaNumber());
        user.setBalance(requestUser.getBalance());

        // Set Default Role (Hardcode biar aman)
        user.setRole("ROLE_USER");

        userRepository.save(user);
    }
}