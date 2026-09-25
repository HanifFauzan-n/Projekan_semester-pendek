package com.example.kartu.services;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual top up flow (v1): the customer files a request, an admin verifies the transfer
 * and approves or rejects it. Nothing is ever credited automatically here; automatic
 * top up goes through Xendit (PaymentService) and is credited only by its webhook.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TopUpService {

    private final UserRepository userRepository;

    private final TopUpRepository topUpRepository;

    private final EmailService emailService;

    public User getUser(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void processTopUp(String username, Double amount) throws Exception {
        if (amount == null || amount < 10000) {
            throw new Exception("Minimal top up adalah Rp 10.000");
        }

        User user = getUser(username);

        TopUp topUp = new TopUp(user, amount);
        topUp.setStatus(TransactionStatus.PENDING);
        topUp.setDate(LocalDateTime.now());
        topUpRepository.save(topUp);
    }

    /** Admin approves a pending manual request: status flips first, then the balance is credited. */
    @Transactional
    public void approveManual(Integer id) {
        TopUp topUp = topUpRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Top up tidak ditemukan."));
        User owner = topUp.getUser();
        Integer userId = owner.getId();
        int amount = topUp.getAmount().intValue();

        if (topUpRepository.resolvePendingManual(id, TransactionStatus.SUCCESS) == 0) {
            throw new IllegalStateException(
                    "Top up ini sudah diproses atau merupakan pembayaran Xendit, tidak bisa disetujui manual.");
        }
        userRepository.creditBalance(userId, amount);
        log.info("[ADMIN] Top up manual id={} disetujui, saldo user id={} bertambah {}.", id, userId, amount);

        long newBalance = userRepository.findBalanceById(userId);
        EmailService.afterCommit(() -> emailService.sendTopUpSuccess(owner.getEmail(), owner.getUsername(), amount,
                "Top up manual (diverifikasi admin)", newBalance, "TOPUP-" + id, LocalDateTime.now()));
    }

    @Transactional
    public void rejectManual(Integer id) {
        if (!topUpRepository.existsById(id)) {
            throw new IllegalArgumentException("Top up tidak ditemukan.");
        }
        if (topUpRepository.resolvePendingManual(id, TransactionStatus.FAILED) == 0) {
            throw new IllegalStateException(
                    "Top up ini sudah diproses atau merupakan pembayaran Xendit, tidak bisa ditolak manual.");
        }
        log.info("[ADMIN] Top up manual id={} ditolak.", id);
    }
}
