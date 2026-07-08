package com.example.kartu.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.User;

public interface UserRepository extends JpaRepository <User,Integer>{
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndRecoveryKey(String username, String recoveryKey);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndUsernameNot(String phoneNumber, String username);
}
