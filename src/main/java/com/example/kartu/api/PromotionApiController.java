package com.example.kartu.api;

import com.example.kartu.enums.PurchaseTarget;
import com.example.kartu.models.FlashSale;
import com.example.kartu.models.Voucher;
import com.example.kartu.repositories.FlashSaleRepository;
import com.example.kartu.repositories.VoucherRepository;
import com.example.kartu.repositories.VoucherUsageRepository;
import com.example.kartu.models.User;
import com.example.kartu.services.UserService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionApiController {

    private final FlashSaleRepository flashSaleRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final UserService userService;

    @GetMapping("/flash-sales/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveFlashSales() {
        LocalDateTime now = LocalDateTime.now();
        List<FlashSale> activeSales = flashSaleRepository.findActiveFlashSales(now);

        List<Map<String, Object>> result = activeSales.stream()
                .filter(fs -> PurchaseTarget.of(fs.getProduct()) != null) // same catalog as /products
                .map(fs -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fs.getId());
            map.put("productId", fs.getProduct().getId());
            map.put("productName", fs.getProduct().getName());
            map.put("originalPrice", fs.getProduct().getPrice());
            map.put("flashPrice", fs.getFlashPrice());
            map.put("startAt", fs.getStartAt());
            map.put("endAt", fs.getEndAt());
            map.put("remainingSeconds", Math.max(0, Duration.between(now, fs.getEndAt()).getSeconds()));
            map.put("quota", fs.getQuota());
            map.put("soldCount", fs.getSoldCount());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/vouchers/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveVouchers() {
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers = voucherRepository.findByActiveTrue().stream()
                .filter(v -> v.isValidFor(null))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = vouchers.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("code", v.getCode());
            map.put("discountType", v.getDiscountType());
            map.put("discountValue", v.getDiscountValue());
            map.put("minPurchase", v.getMinPurchase());
            map.put("maxDiscount", v.getMaxDiscount());
            map.put("usageLimit", v.getUsageLimit());
            map.put("usedCount", v.getUsedCount());
            map.put("remainingQuota", v.getStock());
            map.put("startAt", v.getStartAt());
            map.put("endAt", v.getEndAt());
            map.put("remainingSeconds", v.getEndAt() != null ? Math.max(0, Duration.between(now, v.getEndAt()).getSeconds()) : null);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/vouchers/validate")
    public ResponseEntity<?> validateVoucher(@RequestBody Map<String, Object> body, Authentication authentication) {
        String code = (String) body.get("code");
        Number amountNum = (Number) body.get("amount");
        double amount = amountNum != null ? amountNum.doubleValue() : 0.0;

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "Kode voucher wajib diisi."));
        }

        Optional<Voucher> vOpt = voucherRepository.findByCode(code.trim().toUpperCase());
        if (vOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Kode voucher '" + code.trim().toUpperCase() + "' tidak ditemukan."));
        }

        Voucher v = vOpt.get();
        if (!v.isActive()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Voucher sudah dinonaktifkan."));
        }

        LocalDateTime now = LocalDateTime.now();
        if (v.getStartAt() != null && now.isBefore(v.getStartAt())) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Voucher belum dapat digunakan saat ini."));
        }
        if (v.getEndAt() != null && now.isAfter(v.getEndAt())) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Voucher sudah kedaluwarsa."));
        }

        if (v.getUsageLimit() != null && v.getUsedCount() != null && v.getUsedCount() >= v.getUsageLimit()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Kuota pemakaian voucher ini sudah habis."));
        }

        Optional<User> current = userService.findCurrentUser(authentication);
        if (current.isPresent() && voucherUsageRepository.existsByVoucherIdAndUserId(v.getId(), current.get().getId())) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Voucher ini sudah pernah Anda pakai."));
        }

        if (v.getMinPurchase() != null && v.getMinPurchase() > 0 && amount < v.getMinPurchase()) {
            return ResponseEntity.ok(Map.of("valid", false, "message", String.format("Minimal transaksi untuk voucher ini adalah Rp %,.0f (nominal transaksi Anda: Rp %,.0f).", v.getMinPurchase(), amount)));
        }

        double discount = v.calculateDiscount(amount);
        double finalAmount = Math.max(0, amount - discount);

        Map<String, Object> resp = new HashMap<>();
        resp.put("valid", true);
        resp.put("code", v.getCode());
        resp.put("discountType", v.getDiscountType());
        resp.put("discountValue", v.getDiscountValue());
        resp.put("discountAmount", discount);
        resp.put("finalAmount", finalAmount);
        resp.put("minPurchase", v.getMinPurchase());
        resp.put("maxDiscount", v.getMaxDiscount());
        resp.put("message", "Voucher " + v.getCode() + " berhasil diterapkan! Potongan diskon: Rp " + String.format("%,.0f", discount));

        return ResponseEntity.ok(resp);
    }
}
