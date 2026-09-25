package com.example.kartu.repositories;

import com.example.kartu.models.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    boolean existsByVoucherIdAndUserId(Integer voucherId, Integer userId);
}
