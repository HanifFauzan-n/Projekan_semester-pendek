package com.example.kartu.services;

import com.example.kartu.models.TransactionHistory;
import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.Product;
import com.example.kartu.models.User;
import com.example.kartu.models.Voucher;
import com.example.kartu.repositories.TransactionHistoryRepository;
import com.example.kartu.repositories.ProductRepository;
import com.example.kartu.repositories.UserRepository;
import com.example.kartu.repositories.VoucherRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionHistoryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    // Method untuk generate Transaction ID unik (Contoh: FLC-20260312-A7X9)
    private String generateUniqueTransactionId() {
        String newId;
        boolean exists;
        do {
            String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            StringBuilder randomPart = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                randomPart.append(CHARS.charAt(random.nextInt(CHARS.length())));
            }
            newId = "FLC-" + datePart + "-" + randomPart;

            // Validasi ke database: pastikan belum pernah ada
            exists = transactionHistoryRepository.existsByTransactionId(newId);
        } while (exists); // Jika ada yang sama, ulangi generate

        return newId;
    }

    // Method untuk generate SN unik (16 digit angka)
    private String generateUniqueSN() {
        String sn;
        boolean exists;
        do {
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                sb.append(random.nextInt(10));
            }
            sn = sb.toString();
            exists = transactionHistoryRepository.existsBySerialNumber(sn);
        } while (exists);

        return sn;
    }

    @Transactional(rollbackFor = Exception.class)
    public void purchaseProduct(Integer productId, String username, String voucherCode) throws Exception {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        Double finalPrice = Double.valueOf(product.getPrice());

        // 2. Cek Voucher (Jika user memasukkan kode)
        if (voucherCode != null && !voucherCode.isEmpty()) {
            Voucher voucher = voucherRepository.findByCode(voucherCode)
                    .orElseThrow(() -> new Exception("Kode Voucher tidak valid!"));

            if (voucher.getStock() <= 0 || !voucher.isActive()) {
                throw new Exception("Voucher sudah habis atau tidak aktif.");
            }

            // Potong Harga
            finalPrice = finalPrice - voucher.getDiscountAmount();
            if (finalPrice < 0)
                finalPrice = 0.0; // Mencegah harga minus

            // Kurangi Stok Voucher
            voucher.setStock(voucher.getStock() - 1);
            voucherRepository.save(voucher);
        }

        // 1. Siapkan Object History Awal
        TransactionHistory history = new TransactionHistory();
        history.setUser(user);
        history.setProduct(product);
        history.setTimestamp(LocalDateTime.now());
        history.setStatus(TransactionStatus.PENDING); // Status awal
        history.setTransactionId(generateUniqueTransactionId());
        history.setSerialNumber(generateUniqueSN());

        try {
            // 2. Lakukan Validasi Bisnis
            if (product.getStock() <= 0) {
                throw new Exception("Stok Habis");
            }
            if (user.getBalance() < finalPrice) {
                throw new Exception("Saldo Tidak Cukup");
            }

            user.setBalance((int) (user.getBalance() - finalPrice));
            product.setStock(product.getStock() - 1);

            // Simpan perubahan data master
            userRepository.save(user);
            productRepository.save(product);

            // 4. Update Status Jadi SUCCESS
            history.setStatus(TransactionStatus.SUCCESS);
            history.setAmountPaid(finalPrice);
            transactionHistoryRepository.save(history); // Simpan riwayat sukses

        } catch (Exception e) {
            // 5. JIKA GAGAL -> Catat sebagai FAILED
            history.setStatus(TransactionStatus.FAILED);
            transactionHistoryRepository.save(history); // Simpan riwayat gagal

            // Lempar error lagi agar Controller tahu dan bisa menampilkan pesan ke User
            throw e;
        }
    }

    @Transactional
    public Long calculateTotalRevenue() {
        List<TransactionHistory> transactions = transactionHistoryRepository.findAll();

        // Menjumlahkan dari field amountPaid yang ada di riwayat transaksi
        return (long) transactions.stream()
                .filter(t -> t.getAmountPaid() != null) // Safety check agar tidak error jika ada data null
                .mapToDouble(TransactionHistory::getAmountPaid)
                .sum();
    }

    // Tambahkan ini di TransactionHistoryService
    public List<TransactionHistory> getAllTransactionsDesc() {
        return transactionHistoryRepository.findAllByOrderByTimestampDesc();
    }

    // Tambahkan method ini di dalam class TransactionHistoryService yang sudah ada
    public List<TransactionHistory> getTransactionHistoryByUser(User user) {
        return transactionHistoryRepository.findByUserIdOrderByTimestampDesc(user.getId());
    }
}