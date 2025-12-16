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

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "dana_number")
    private String danaNumber;

    @Column(name = "balance")
    private Integer balance;

    private String role;
}