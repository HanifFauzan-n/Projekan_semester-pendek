package com.example.kartu.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;
    private String password;

    @Column(name = "nomer")
    private String phoneNumber;

    @Column(name = "Nomer_dana")
    private String danaNumber;

    @Column(name = "saldo")
    private Integer balance;

    private String role;
}