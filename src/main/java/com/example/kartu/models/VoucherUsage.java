package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * SKPL-F04/F07/F13: one row per (voucher, customer). The UNIQUE constraint in
 * docs/sql/004_voucher_usages.sql is what stops the same customer from using a voucher twice.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "voucher_usages")
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private TransactionHistory transaction;

    @Column(name = "used_at", nullable = false, updatable = false)
    private OffsetDateTime usedAt;

    public VoucherUsage(Voucher voucher, User user, TransactionHistory transaction) {
        this.voucher = voucher;
        this.user = user;
        this.transaction = transaction;
    }

    @PrePersist
    void onCreate() {
        usedAt = OffsetDateTime.now();
    }
}
