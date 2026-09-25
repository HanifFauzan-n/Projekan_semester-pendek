package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * SKPL-F14: Entity untuk mengelola promo Flash Sale berbatas waktu.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "flash_sales")
public class FlashSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "flash_price", nullable = false)
    private Integer flashPrice;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "quota")
    private Integer quota = 100;

    @Column(name = "sold_count")
    private Integer soldCount = 0;

    @Column(name = "is_active")
    private boolean active = true;

    public boolean isRunning() {
        LocalDateTime now = LocalDateTime.now();
        return active && now.isAfter(startAt) && now.isBefore(endAt) && (quota == null || soldCount < quota);
    }
}
