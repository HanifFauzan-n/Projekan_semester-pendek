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

    private LocalDateTime timestamp = LocalDateTime.now();
    // TransactionHistory.java
    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;
    @Enumerated(EnumType.STRING) // Menyimpan sebagai teks "SUCCESS"/"FAILED" di database
    private TransactionStatus status;

    @ManyToOne
    @JoinColumn(name = "purchaser")
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_product")
    private Product product;

    private Double amountPaid;
}