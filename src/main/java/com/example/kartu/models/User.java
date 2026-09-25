package com.example.kartu.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

// @DynamicUpdate: only changed columns are written. Balance is changed with atomic
// UPDATE queries (UserRepository), so saving this entity for other reasons (OAuth login,
// profile edit) must never write back a stale balance.
@Entity
@Data
@DynamicUpdate
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;
    private String password;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    // Wrong guesses against the current OTP; the code is discarded at the limit.
    @Column(name = "otp_attempts")
    private Integer otpAttempts;

    // Used to enforce a cooldown between OTP emails.
    @Column(name = "otp_sent_at")
    private LocalDateTime otpSentAt;

    @Column(name = "auth_provider")
    private String authProvider = "LOCAL"; // "LOCAL" or "GOOGLE"

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "phone_number")
    private String phoneNumber;


    @Column(name = "balance")
    private Integer balance;

    private String status = "ACTIVE";

    // Untuk melihat kapan pengguna ini mendaftar
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    private String role;
}