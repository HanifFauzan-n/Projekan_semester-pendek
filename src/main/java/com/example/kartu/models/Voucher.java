package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * SKPL-F04 / SKPL-F07 / SKPL-F13:
 * Entity Voucher Diskon Lanjutan (Tipe Persentase / Nominal, Min Transaksi, Max Potongan, Kuota, dan Masa Berlaku).
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code; // Contoh: "DISKON50"

    @Column(name = "discount_type", nullable = false)
    private String discountType = "NOMINAL"; // "PERCENT" atau "NOMINAL"

    @Column(name = "discount_value", nullable = false)
    private Double discountValue = 0.0; // 50 (untuk 50%) atau 10000 (untuk Rp 10.000)

    @Column(name = "min_purchase")
    private Double minPurchase = 0.0; // Minimal transaksi (cth: 50.000)

    @Column(name = "max_discount")
    private Double maxDiscount; // Batas maksimal diskon jika tipe PERCENT (cth: 20.000)

    @Column(name = "usage_limit")
    private Integer usageLimit = 100; // Kuota maksimal pemakaian

    @Column(name = "used_count")
    private Integer usedCount = 0; // Jumlah yang sudah terpakai

    @Column(name = "start_at")
    private LocalDateTime startAt; // Waktu mulai berlaku

    @Column(name = "end_at")
    private LocalDateTime endAt; // Waktu kedaluwarsa

    @Column(name = "is_active")
    private boolean active = true;

    // Backward compatibility setter/getter untuk field lama jika ada pemanggil lama
    public Double getDiscountAmount() {
        return discountValue;
    }

    public void setDiscountAmount(Double amount) {
        this.discountValue = amount;
    }

    public Integer getStock() {
        if (usageLimit == null) return 999;
        return Math.max(0, usageLimit - (usedCount != null ? usedCount : 0));
    }

    public void setStock(Integer stock) {
        this.usageLimit = stock;
    }

    /**
     * Menghitung nilai potongan diskon berdasarkan harga transaksi
     */
    public Double calculateDiscount(Double amount) {
        if (amount == null || amount <= 0) return 0.0;

        if ("PERCENT".equalsIgnoreCase(discountType)) {
            double rawDiscount = (amount * discountValue) / 100.0;
            if (maxDiscount != null && maxDiscount > 0 && rawDiscount > maxDiscount) {
                return maxDiscount;
            }
            return rawDiscount;
        } else {
            // NOMINAL
            return Math.min(discountValue, amount);
        }
    }

    /**
     * Mengecek apakah voucher masih valid digunakan untuk nominal tertentu
     */
    public boolean isValidFor(Double amount) {
        LocalDateTime now = LocalDateTime.now();

        if (!active) return false;
        if (startAt != null && now.isBefore(startAt)) return false;
        if (endAt != null && now.isAfter(endAt)) return false;
        if (usageLimit != null && usedCount != null && usedCount >= usageLimit) return false;
        if (amount != null && minPurchase != null && minPurchase > 0 && amount < minPurchase) return false;

        return true;
    }
}
