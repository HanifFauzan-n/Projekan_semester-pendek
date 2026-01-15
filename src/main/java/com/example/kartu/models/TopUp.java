package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    private LocalDateTime date; // Saya ganti 'transactionDate' jadi 'date' biar simpel


    // Constructor Helper
    public TopUp(User user, Double amount) {
        this.user = user;
        this.amount = amount;
        this.date = LocalDateTime.now();
    }
}