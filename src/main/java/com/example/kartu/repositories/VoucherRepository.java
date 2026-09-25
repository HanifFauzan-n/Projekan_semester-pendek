package com.example.kartu.repositories;

import com.example.kartu.models.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByCode(String code);
    List<Voucher> findByActiveTrue();

    /** Uses one voucher quota atomically; 0 means the quota ran out in the meantime. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Voucher v SET v.usedCount = COALESCE(v.usedCount, 0) + 1 "
            + "WHERE v.id = :id AND (v.usageLimit IS NULL OR COALESCE(v.usedCount, 0) < v.usageLimit)")
    int claimUsage(@Param("id") Integer id);
}
