package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.kartu.enums.TransactionStatus;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Data
@Table(name = "top_up") // Nama tabel di database biar rapi (jamak)
public class TopUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double amount;
    
    private LocalDateTime date; // Saya ganti 'transactionDate' jadi 'date' agar sederhana

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    // --- Xendit payment gateway (SKPL-F05) ---
    // All nullable: manual top up rows created before v2 never fill these.
    // A null externalId is what marks a row as belonging to the old manual flow,
    // which is what TopUpService.autoApproveTopUp() uses to skip Xendit rows.

    @Column(unique = true)
    private String externalId;

    private String xenditInvoiceId;

    @Column(length = 512)
    private String invoiceUrl;

    private String paymentMethod;

    private String paymentChannel;

    private LocalDateTime paidAt;


    // Konstruktor Pembantu
    public TopUp(User user, Double amount) {
        this.user = user;
        this.amount = amount;
        this.date = LocalDateTime.now();
    }
}