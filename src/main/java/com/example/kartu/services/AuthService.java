package com.example.kartu.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void registerUser(User user) throws Exception {
        // == FULL VALIDATION START ==

        // 1. Username Validation
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new Exception("Username is already taken, please choose another.");
        }
        if (user.getUsername().equalsIgnoreCase("Hanif18")) {
            throw new Exception("This username is not valid.");
        }
        if (user.getUsername().length() < 8) {
            throw new Exception("Username must be at least 8 characters long.");
        }

        // 2. Password Validation
        if (user.getPassword().length() < 8) {
            throw new Exception("Password must be at least 8 characters long.");
        }

        // 3. Phone Number Validation
        if (user.getPhoneNumber() == null || user.getPhoneNumber().length() < 12 || user.getPhoneNumber().length() > 15) {
            throw new Exception("Phone number length must be between 12 and 15 digits.");
        }
        if (!user.getPhoneNumber().startsWith("08")) {
            throw new Exception("Phone number must start with '08'.");
        }
        
        // 4. DANA Number Validation
        if (user.getDanaNumber() == null || user.getDanaNumber().length() < 12 || user.getDanaNumber().length() > 15) {
            throw new Exception("DANA number length must be between 12 and 15 digits.");
        }
        if (!user.getDanaNumber().startsWith("08")) {
            throw new Exception("DANA number must start with '08'.");
        }

        // == FULL VALIDATION END ==

        // Encrypt password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Set default role
        user.setRole("ROLE_USER");
        
        userRepository.save(user);
    }
}