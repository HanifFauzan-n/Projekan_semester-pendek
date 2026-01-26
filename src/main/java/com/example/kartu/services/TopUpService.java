package com.example.kartu.services;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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

        // 4. Simpan Bukti/Riwayat (Entity TopUp)
        TopUp topUp = new TopUp(user, amount);
        topUp.setStatus(TransactionStatus.PENDING);
        topUp.setDate(LocalDateTime.now());
        topUpRepository.save(topUp);
    }

    // 2. FITUR BARU: Robot Pengecek Otomatis (Scheduler)
    // Jalan setiap 60.000 ms (1 menit)
    @Scheduled(fixedRate = 60000)
    @Transactional // Biar aman kalau ada error di tengah jalan
    public void autoApproveTopUp() {
        System.out.println("[SCHEDULER] Mengecek top up pending...");

        // Ambil semua yang PENDING
        List<TopUp> pendingList = topUpRepository.findByStatus(TransactionStatus.PENDING);

        LocalDateTime now = LocalDateTime.now();

        for (TopUp topUp : pendingList) {
            // Hitung selisih waktu (dalam menit)
            long minutesSinceRequest = ChronoUnit.MINUTES.between(topUp.getDate(), now);

            // Jika sudah lewat 3 menit (Ganti angka 3 kalau mau lebih cepat saat demo,
            // misal 1 menit)
            if (minutesSinceRequest >= 1) {

                // 1. Tambah Saldo User
                User user = topUp.getUser();
                user.setBalance(user.getBalance() + topUp.getAmount().intValue());
                userRepository.save(user);

                // 2. Ubah Status jadi SUCCESS
                topUp.setStatus(TransactionStatus.SUCCESS);
                topUpRepository.save(topUp);

                log.info("TopUp ID " + topUp.getId() + " BERHASIL diproses otomatis.");
            }
        }
    }

    public List<TopUp> getAllTopUpsDesc() {
        return topUpRepository.findAllByOrderByDateDesc();
    }

    // Tambahkan method ini di dalam class TopUpService yang sudah ada
    public List<TopUp> getTopUpHistoryByUser(User user) {
        return topUpRepository.findByUserIdOrderByDateDesc(user.getId());
    }
}