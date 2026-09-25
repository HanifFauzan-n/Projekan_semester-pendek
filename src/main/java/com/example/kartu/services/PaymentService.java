package com.example.kartu.services;

import com.example.kartu.dto.request.XenditCallbackRequest;
import com.example.kartu.dto.response.XenditInvoiceResponse;
import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import com.example.kartu.models.User;
import com.example.kartu.repositories.TopUpRepository;
import com.example.kartu.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Business layer for automatic top up through Xendit (SKPL-F05).
 *
 * XenditService only speaks HTTP; this class owns the rules that protect the
 * balance: the top up is created PENDING, and the balance is credited ONLY when
 * a webhook arrives whose token, status and amount all check out. The customer
 * coming back to the success redirect URL never credits anything, because that
 * URL can be opened manually by anyone.
 *
 * The old manual top up flow in TopUpService is untouched and still works as a
 * fallback. The two are kept apart by TopUp.externalId being null for manual rows.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final double MIN_TOP_UP = 10000;

    private final TopUpRepository topUpRepository;
    private final UserRepository userRepository;
    private final XenditService xenditService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Creates a PENDING top up and asks Xendit for an invoice.
     *
     * @return the Xendit payment page URL the customer must be redirected to
     * @throws Exception when the amount is below the minimum
     * @throws XenditService.XenditException when Xendit is not configured or rejects the request
     */
    @Transactional
    public String createInvoice(String usernameOrEmail, Double amount) throws Exception {
        if (amount == null || amount < MIN_TOP_UP) {
            throw new Exception("Minimum Top Up is Rp 10,000");
        }

        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Saved first so the generated id can be part of the external id.
        TopUp topUp = new TopUp(user, amount);
        topUp.setStatus(TransactionStatus.PENDING);
        topUp.setDate(LocalDateTime.now());
        topUpRepository.save(topUp);

        String externalId = xenditService.buildExternalId(topUp.getId().longValue());
        topUp.setExternalId(externalId);

        String returnUrl = frontendUrl + "/topup/status/" + externalId;
        String payerEmail = user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : null;
        XenditInvoiceResponse invoice = xenditService.createInvoice(
                externalId,
                BigDecimal.valueOf(amount),
                payerEmail,
                "Top up saldo Zelatan Cell",
                returnUrl,
                returnUrl);

        topUp.setXenditInvoiceId(invoice.getId());
        topUp.setInvoiceUrl(invoice.getInvoiceUrl());
        topUpRepository.save(topUp);

        return invoice.getInvoiceUrl();
    }

    /**
     * Processes a Xendit webhook. The caller must have verified the
     * X-CALLBACK-TOKEN header before reaching this method.
     *
     * Never throws for an unknown or already-paid top up: the controller answers
     * 200 in those cases so Xendit stops retrying a callback we deliberately ignore.
     */
    @Transactional
    public void handleCallback(XenditCallbackRequest callback) {
        if (callback == null || callback.getExternalId() == null) {
            log.warn("Webhook Xendit tanpa external_id, diabaikan.");
            return;
        }

        Optional<TopUp> found = topUpRepository.findByExternalIdForUpdate(callback.getExternalId());
        if (found.isEmpty()) {
            log.warn("Webhook Xendit untuk externalId={} yang tidak dikenal, diabaikan.",
                    callback.getExternalId());
            return;
        }
        TopUp topUp = found.get();

        // Idempotent: Xendit can deliver the same callback more than once.
        if (topUp.getStatus() == TransactionStatus.SUCCESS) {
            log.info("Webhook Xendit externalId={} sudah pernah diproses, saldo tidak ditambah lagi.",
                    callback.getExternalId());
            return;
        }

        if (callback.isExpired()) {
            topUp.setStatus(TransactionStatus.FAILED);
            topUpRepository.save(topUp);
            log.info("Invoice Xendit externalId={} kedaluwarsa.", callback.getExternalId());
            return;
        }

        if (!callback.isPaid()) {
            log.info("Webhook Xendit externalId={} status={} belum lunas, tidak ada perubahan saldo.",
                    callback.getExternalId(), callback.getStatus());
            return;
        }

        BigDecimal expected = BigDecimal.valueOf(topUp.getAmount());
        BigDecimal paid = callback.getPaidAmount();
        if (paid == null || paid.compareTo(expected) != 0) {
            // Left PENDING on purpose so an admin can review it instead of the
            // balance being credited with the wrong number.
            log.error("Nominal webhook Xendit tidak cocok untuk externalId={}. "
                    + "Tersimpan={} dibayar={}. Saldo TIDAK ditambah, perlu ditinjau admin.",
                    callback.getExternalId(), expected, paid);
            return;
        }

        User user = topUp.getUser();
        userRepository.creditBalance(user.getId(), topUp.getAmount().intValue());

        topUp.setStatus(TransactionStatus.SUCCESS);
        topUp.setPaidAt(toLocalDateTime(callback.parsePaidAt()));
        topUp.setPaymentMethod(callback.getPaymentMethod());
        topUp.setPaymentChannel(callback.getPaymentChannel());
        topUpRepository.save(topUp);

        log.info("Top up Xendit externalId={} lunas. Saldo {} bertambah {}.",
                callback.getExternalId(), user.getUsername(), topUp.getAmount());

        long newBalance = userRepository.findBalanceById(user.getId());
        String method = "Xendit" + (topUp.getPaymentChannel() != null ? " " + topUp.getPaymentChannel() : "");
        LocalDateTime paidAt = topUp.getPaidAt();
        EmailService.afterCommit(() -> emailService.sendTopUpSuccess(user.getEmail(), user.getUsername(),
                topUp.getAmount().longValue(), method, newBalance, topUp.getExternalId(), paidAt));
    }

    public Optional<TopUp> findByExternalId(String externalId) {
        return topUpRepository.findByExternalId(externalId);
    }

    /** Falls back to server time when Xendit sends a paid_at we cannot parse. */
    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
