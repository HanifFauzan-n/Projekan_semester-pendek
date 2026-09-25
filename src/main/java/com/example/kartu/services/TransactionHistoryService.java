package com.example.kartu.services;

import com.example.kartu.models.FlashSale;
import com.example.kartu.models.TransactionHistory;
import com.example.kartu.enums.PurchaseTarget;
import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.Product;
import com.example.kartu.models.User;
import com.example.kartu.models.Voucher;
import com.example.kartu.models.VoucherUsage;
import com.example.kartu.repositories.FlashSaleRepository;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.repositories.VoucherRepository;
import com.example.kartu.repositories.VoucherUsageRepository;

import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionHistoryService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final EmailService emailService;
    private final VoucherUsageRepository voucherUsageRepository;

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    // Metode untuk membuat ID transaksi unik (contoh: ZLC-20260312-A7X9)
    private String generateUniqueTransactionId() {
        String newId;
        boolean exists;
        do {
            String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            StringBuilder randomPart = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                randomPart.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            newId = "ZLC-" + datePart + "-" + randomPart;

            // Validasi ke database: pastikan belum pernah ada
            exists = transactionHistoryRepository.existsByTransactionId(newId);
        } while (exists); // Jika ada yang sama, ulangi pembuatan

        return newId;
    }

    // Metode untuk membuat SN unik (16 digit angka; token PLN 20 digit)
    private String generateUniqueSN(int length) {
        String sn;
        boolean exists;
        do {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(random.nextInt(10));
            }
            sn = sb.toString();
            exists = transactionHistoryRepository.existsBySerialNumber(sn);
        } while (exists);

        return sn;
    }

    /**
     * Purchase with balance. Every shared counter (flash sale quota, voucher quota, balance,
     * stock) is changed with a single conditional UPDATE, never read-modify-write on a loaded
     * entity, so concurrent purchases cannot oversell or overspend. Any failure rolls the
     * whole purchase back, including quota already claimed.
     *
     * Pricing order: flash sale price first, then the voucher is applied to that price.
     *
     * customerNumber is the destination: phone number for pulsa/data, meter number for PLN.
     * Products without a destination (accessories) record the buyer's own phone number.
     */
    @Transactional(rollbackFor = Exception.class)
    public TransactionHistory purchaseProduct(Integer productId, String usernameOrEmail, String voucherCode,
                                              String customerNumber) throws Exception {

        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new Exception("User tidak ditemukan"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Produk tidak ditemukan"));

        PurchaseTarget target = PurchaseTarget.of(product);
        String destination = target == null ? user.getPhoneNumber() : target.normalize(customerNumber);

        double basePrice = product.getPrice();
        double activePrice = basePrice;

        // 1. Flash sale: the cheapest running one whose quota can still be claimed.
        for (FlashSale fs : flashSaleRepository.findRunningForProduct(productId, LocalDateTime.now())) {
            if (flashSaleRepository.claimSlot(fs.getId()) == 1) {
                activePrice = fs.getFlashPrice();
                break;
            }
        }
        double flashDiscount = basePrice - activePrice;

        // 2. Voucher, applied to the price after flash sale.
        double voucherDiscount = 0.0;
        Voucher appliedVoucher = null;
        if (voucherCode != null && !voucherCode.trim().isEmpty()) {
            Voucher voucher = voucherRepository.findByCode(voucherCode.trim().toUpperCase())
                    .orElseThrow(() -> new Exception("Kode voucher '" + voucherCode + "' tidak valid!"));

            if (!voucher.isActive()) {
                throw new Exception("Voucher " + voucher.getCode() + " sudah tidak aktif.");
            }

            LocalDateTime now = LocalDateTime.now();
            if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) {
                throw new Exception("Voucher " + voucher.getCode() + " belum dapat digunakan.");
            }
            if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
                throw new Exception("Voucher " + voucher.getCode() + " sudah kedaluwarsa.");
            }

            if (voucher.getMinPurchase() != null && voucher.getMinPurchase() > 0 && activePrice < voucher.getMinPurchase()) {
                throw new Exception(String.format(ID, "Minimal transaksi untuk voucher %s adalah Rp %,.0f (harga produk saat ini: Rp %,.0f).",
                        voucher.getCode(), voucher.getMinPurchase(), activePrice));
            }

            if (voucherUsageRepository.existsByVoucherIdAndUserId(voucher.getId(), user.getId())) {
                throw new Exception("Voucher " + voucher.getCode() + " sudah pernah Anda pakai. Satu voucher hanya bisa dipakai sekali per pelanggan.");
            }

            if (voucherRepository.claimUsage(voucher.getId()) == 0) {
                throw new Exception("Kuota pemakaian voucher " + voucher.getCode() + " sudah habis.");
            }

            voucherDiscount = voucher.calculateDiscount(activePrice);
            appliedVoucher = voucher;
        }

        double finalPrice = Math.max(0.0, activePrice - voucherDiscount);
        int charge = (int) Math.round(finalPrice);

        // 3. Balance: check and debit in one statement.
        if (userRepository.debitBalance(user.getId(), charge) == 0) {
            throw new Exception("Saldo tidak mencukupi. Diperlukan Rp " + String.format(ID, "%,d", charge)
                    + ", saldo Anda: Rp " + String.format(ID, "%,d", userRepository.findBalanceById(user.getId())));
        }

        // 4. Stock: same pattern.
        if (productRepository.decrementStock(productId) == 0) {
            throw new Exception("Stok produk '" + product.getName() + "' sudah habis.");
        }

        TransactionHistory history = new TransactionHistory();
        history.setUser(user);
        history.setProduct(product);
        history.setCustomer(user.getUsername());
        history.setCustomerNumber(destination);
        history.setTimestamp(LocalDateTime.now());
        history.setStatus(TransactionStatus.SUCCESS);
        history.setTransactionId(generateUniqueTransactionId());
        history.setSerialNumber(generateUniqueSN(target == null ? 16 : target.codeLength()));
        history.setVoucherCode(appliedVoucher != null ? appliedVoucher.getCode() : null);
        history.setDiscountAmount(flashDiscount + voucherDiscount);

        Double cost = product.getCostPrice() != null ? Double.valueOf(product.getCostPrice()) : basePrice * 0.9;
        history.setCostPrice(cost);
        history.setAmountPaid((double) charge);

        TransactionHistory saved = transactionHistoryRepository.save(history);

        if (appliedVoucher != null) {
            try {
                voucherUsageRepository.saveAndFlush(new VoucherUsage(appliedVoucher, user, saved));
            } catch (DataIntegrityViolationException e) {
                // a concurrent purchase by the same customer used it first (UNIQUE voucher_id, user_id)
                throw new Exception("Voucher " + appliedVoucher.getCode() + " sudah pernah Anda pakai.");
            }
        }

        long newBalance = userRepository.findBalanceById(user.getId());
        String voucherUsed = history.getVoucherCode();
        long discountTotal = Math.round(flashDiscount + voucherDiscount);
        EmailService.afterCommit(() -> emailService.sendPurchaseReceipt(user.getEmail(), user.getUsername(),
                product.getName(), target, destination, Math.round(basePrice), discountTotal, voucherUsed,
                charge, newBalance, saved.getTransactionId(), saved.getSerialNumber(), saved.getTimestamp()));
        return saved;
    }

    public List<TransactionHistory> getTransactionHistoryByUser(User user) {
        return transactionHistoryRepository.findByUserIdOrderByTimestampDesc(user.getId());
    }
}
