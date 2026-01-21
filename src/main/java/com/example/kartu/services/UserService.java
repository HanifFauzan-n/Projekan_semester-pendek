package com.example.kartu.services;

import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Method helper untuk mengambil user yang sedang login
    public User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
    
    // Method khusus untuk mengambil saldo user (opsional, jika ingin lebih spesifik)
    public Integer getUserBalance(Principal principal) {
        User user = getCurrentUser(principal);
        return (user != null) ? user.getBalance() : 0;
    }
}