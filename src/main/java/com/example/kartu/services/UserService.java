package com.example.kartu.services;

import com.example.kartu.dto.request.UserProfileRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Di dalam UserService.java
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Method helper untuk mengambil user yang sedang login
    public User getCurrentUser(Principal principal) {
        if (principal == null)
            return null;
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // Method khusus untuk mengambil saldo user (opsional, jika ingin lebih
    // spesifik)
    public Integer getUserBalance(Principal principal) {
        User user = getCurrentUser(principal);
        return (user != null) ? user.getBalance() : 0;
    }

    public void updateUserProfile(String username, UserProfileRequest request) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("User not found"));

        // Update Info Dasar
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDanaNumber() != null && !request.getDanaNumber().isEmpty()) {
            user.setDanaNumber(request.getDanaNumber());
        }

        // Logic Ganti Password (Jika diisi)
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            // Cek password lama harus benar
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new Exception("Password lama salah!");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
    }
}