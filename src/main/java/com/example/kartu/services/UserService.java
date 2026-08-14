package com.example.kartu.services;

import com.example.kartu.dto.request.UserProfileRequest;
import com.example.kartu.models.User;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public User getCurrentUser(Principal principal) {
        if (principal == null)
            return null;
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // Metode khusus untuk mengambil saldo pengguna (opsional, jika ingin lebih
    // spesifik)
    public Integer getUserBalance(Principal principal) {
        User user = getCurrentUser(principal);
        return (user != null) ? user.getBalance() : 0;
    }

    public void updateUserProfile(String username, UserProfileRequest request) throws Exception {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new Exception("User not found"));

    // 1. Validasi & Perbarui Nomor Telepon (Ini tetap butuh pemeriksaan duplikat biar nomor kontak gak kembar)
    if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
        boolean phoneExists = userRepository.existsByPhoneNumberAndUsernameNot(request.getPhoneNumber(), username);
        if (phoneExists) {
            throw new Exception("Phone number is already used by another account");
        }
        user.setPhoneNumber(request.getPhoneNumber());
    }

    // 2. Perbarui Kunci Pemulihan (Bebas, rahasia, dan boleh sama dengan user lain secara tidak sengaja)
    if (request.getRecoveryKey() != null && !request.getRecoveryKey().isEmpty()) {
        user.setRecoveryKey(request.getRecoveryKey());
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
                .orElseThrow(() -> new IllegalArgumentException("User data not found"));
    }

    public void banUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User data not found"));

        // Ubah status menjadi BANNED
        user.setStatus("BANNED");
        userRepository.save(user);
    }

    public void unbanUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User data not found"));

        // Kembalikan status menjadi ACTIVE
        user.setStatus("ACTIVE");
        userRepository.save(user);
    }
}
