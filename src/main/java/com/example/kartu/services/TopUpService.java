package com.example.kartu.services;

import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopUpService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopUpRepository topUpRepository;

    // Helper untuk ambil user
    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void processTopUp(String username, Double amount) throws Exception {
        // 1. Validasi
        if (amount == null || amount < 10000) {
            throw new Exception("Minimal Top Up Rp 10.000");
        }

        // 2. Ambil User
        User user = getUser(username);

        // 3. Update Saldo User (Saldo Akhir)
        double currentBalance = (user.getBalance() == null) ? 0 : user.getBalance();
        user.setBalance((int) (currentBalance + amount));
        userRepository.save(user);

        // 4. Simpan Bukti/Riwayat (Entity TopUp)
        TopUp topUp = new TopUp(user, amount);
        topUpRepository.save(topUp);
    }

    public List<TopUp> getAllTopUpsDesc() {
        return topUpRepository.findAllByOrderByDateDesc();
    }

    // Tambahkan method ini di dalam class TopUpService yang sudah ada
    public List<TopUp> getTopUpHistoryByUser(User user) {
        return topUpRepository.findByUserIdOrderByDateDesc(user.getId());
    }
}