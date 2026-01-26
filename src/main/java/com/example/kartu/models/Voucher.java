package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code; // Contoh: "MAHASISWA"

    @Column(nullable = false)
    private Double discountAmount; // Contoh: 5000.0

    private Integer stock; // Kuota voucher, misal 100

    private boolean isActive = true; // Bisa dimatikan admin kapan saja
}