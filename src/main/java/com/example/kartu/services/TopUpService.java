package com.example.kartu.services;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopUpService {

    private final UserRepository userRepository;

    private final TopUpRepository topUpRepository;

    // Pembantu untuk mengambil pengguna
    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void processTopUp(String username, Double amount) throws Exception {
        // 1. Validasi
        if (amount == null || amount < 10000) {
            throw new Exception("Minimum Top Up is Rp 10,000");
        }

        // 2. Ambil Pengguna
        User user = getUser(username);

        // 4. Simpan Bukti/Riwayat (Entitas TopUp)
        TopUp topUp = new TopUp(user, amount);
        topUp.setStatus(TransactionStatus.PENDING);
        topUp.setDate(LocalDateTime.now());
        topUpRepository.save(topUp);
    }

    // 2. FITUR BARU: Robot Pengecek Otomatis (Penjadwal)
    // Berjalan setiap 60.000 ms (1 menit)
    @Scheduled(fixedRate = 60000)
    @Transactional // Biar aman kalau ada error di tengah jalan
    public void autoApproveTopUp() {
        System.out.println("[SCHEDULER] Checking pending Top Up requests...");

        // Ambil semua yang PENDING
        List<TopUp> pendingList = topUpRepository.findByStatus(TransactionStatus.PENDING);

        LocalDateTime now = LocalDateTime.now();

        for (TopUp topUp : pendingList) {
            // Hitung selisih waktu (dalam menit)
            long minutesSinceRequest = ChronoUnit.MINUTES.between(topUp.getDate(), now);

            // Jika sudah lewat 3 menit (Ganti angka 3 kalau mau lebih cepat saat demo,
            // misal 1 menit)
            if (minutesSinceRequest >= 1) { // posisi pending top up

                // 1. Tambah Saldo Pengguna
                User user = topUp.getUser();
                user.setBalance(user.getBalance() + topUp.getAmount().intValue());
                userRepository.save(user);

                // 2. Ubah Status jadi SUCCESS
                topUp.setStatus(TransactionStatus.SUCCESS);
                topUpRepository.save(topUp);

                log.info("Top Up ID " + topUp.getId() + " processed automatically with SUCCESS status.");
            }
        }
    }

    public List<TopUp> getAllTopUpsDesc() {
        return topUpRepository.findAllByOrderByDateDesc();
    }

    // Tambahkan method ini di dalam kelas TopUpService yang sudah ada
    public List<TopUp> getTopUpHistoryByUser(User user) {
        return topUpRepository.findByUserIdOrderByDateDesc(user.getId());
    }

    @Transactional
    public void cancelTopUp(Integer id) {
        Optional<TopUp> topUpOpt = topUpRepository.findById(id);

        if (topUpOpt.isPresent()) {
            TopUp topUp = topUpOpt.get();

            // Hanya bisa batalkan jika status masih PENDING
            if (topUp.getStatus() == TransactionStatus.PENDING) {
                topUp.setStatus(TransactionStatus.FAILED); // Ubah status jadi FAILED
                topUpRepository.save(topUp);
                log.info("[ADMIN ACTION] Top Up ID {} canceled successfully.", id);
            } else {
                throw new RuntimeException("A completed transaction cannot be canceled.");
            }
        }
    }
}
