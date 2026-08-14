package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.example.kartu.enums.TransactionStatus;

@Entity
@Data
public class TransactionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String customer;
    private String customerNumber;

    private LocalDateTime timestamp = LocalDateTime.now();
    // Berkas TransactionHistory.java
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;
    @Enumerated(EnumType.STRING) // Disimpan sebagai teks "SUCCESS"/"FAILED" di database
    private TransactionStatus status;

    @ManyToOne
    @JoinColumn(name = "purchaser")
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Double amountPaid;
}
