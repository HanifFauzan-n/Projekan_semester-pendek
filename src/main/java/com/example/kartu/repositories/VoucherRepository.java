package com.example.kartu.repositories;

import com.example.kartu.models.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);
}