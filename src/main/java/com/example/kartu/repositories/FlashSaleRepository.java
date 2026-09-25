package com.example.kartu.repositories;

import com.example.kartu.models.FlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, Integer> {

    @Query("SELECT f FROM FlashSale f WHERE f.active = true AND f.startAt <= :now AND f.endAt >= :now")
    List<FlashSale> findActiveFlashSales(LocalDateTime now);

    /** Flash sales of one product that are in their period and still have quota, cheapest first. */
    @Query("SELECT f FROM FlashSale f WHERE f.product.id = :productId AND f.active = true "
            + "AND f.startAt <= :now AND f.endAt > :now "
            + "AND (f.quota IS NULL OR COALESCE(f.soldCount, 0) < f.quota) ORDER BY f.flashPrice ASC")
    List<FlashSale> findRunningForProduct(@Param("productId") Integer productId, @Param("now") LocalDateTime now);

    /** Takes one unit of quota atomically; 0 means the quota ran out in the meantime. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE FlashSale f SET f.soldCount = COALESCE(f.soldCount, 0) + 1 "
            + "WHERE f.id = :id AND (f.quota IS NULL OR COALESCE(f.soldCount, 0) < f.quota)")
    int claimSlot(@Param("id") Integer id);

    /** Flash sales whose period overlaps [from, to), for promo reports. */
    @Query("SELECT f FROM FlashSale f JOIN FETCH f.product WHERE f.startAt < :to AND f.endAt > :from ORDER BY f.startAt")
    List<FlashSale> findOverlapping(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Active flash sales of the same product whose period overlaps [startAt, endAt). */
    @Query("SELECT COUNT(f) FROM FlashSale f WHERE f.product.id = :productId AND f.active = true "
            + "AND f.id <> :excludeId AND f.startAt < :endAt AND f.endAt > :startAt")
    long countOverlapping(@Param("productId") Integer productId, @Param("excludeId") Integer excludeId,
                          @Param("startAt") LocalDateTime startAt, @Param("endAt") LocalDateTime endAt);
}
