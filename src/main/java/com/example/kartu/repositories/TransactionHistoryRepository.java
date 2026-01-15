package com.example.kartu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kartu.models.TransactionHistory;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Integer> {
    List<TransactionHistory> findByUserId(Integer integer);

    List<TransactionHistory> findAllByOrderByTimestampDesc();

}
