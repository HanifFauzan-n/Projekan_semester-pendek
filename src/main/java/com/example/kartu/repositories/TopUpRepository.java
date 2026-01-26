package com.example.kartu.repositories;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopUpRepository extends JpaRepository<TopUp, Integer> {
    // Untuk menampilkan riwayat di profile nanti
    List<TopUp> findByUserIdOrderByDateDesc(Integer userId);

    List<TopUp> findAllByOrderByDateDesc();

    List<TopUp> findByStatus(TransactionStatus status);
}