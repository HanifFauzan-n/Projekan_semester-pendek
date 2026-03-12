package com.example.kartu.services;

import com.example.kartu.dto.request.UserProfileRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;

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

        // Logic Ganti Password
        if (StringUtils.hasText(request.getNewPassword())) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new IllegalArgumentException("Password lama harus diisi!");
            }

            // Cek password lama harus benar
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Password lama salah!");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
    }

    // Untuk Admin: Melihat daftar seluruh pengguna
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Untuk Admin: Melihat detail informasi pengguna berdasarkan ID
    public User getUserDetailsById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data pengguna tidak ditemukan"));
    }

    public void banUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data pengguna tidak ditemukan"));

        // Ubah status menjadi BANNED
        user.setStatus("BANNED");
        userRepository.save(user);
    }

    public void unbanUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data pengguna tidak ditemukan"));

        // Kembalikan status menjadi ACTIVE
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }
}