package com.example.kartu.repositories;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.kartu.models.ProductOrder;
import com.example.kartu.models.ProductOrder.Status;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

    @EntityGraph(attributePaths = {"user", "product"})
    Optional<ProductOrder> findByExternalId(String externalId);

    /**
     * PENDING -> PAID in one statement. Returns 0 when the order was already handled, so a webhook
     * that Xendit delivers twice cannot credit the balance twice.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE ProductOrder o SET o.status = com.example.kartu.models.ProductOrder.Status.PAID, "
            + "o.paidAt = :paidAt, o.paymentChannel = :channel "
            + "WHERE o.id = :id AND o.status = com.example.kartu.models.ProductOrder.Status.PENDING")
    int markPaid(@Param("id") Long id, @Param("paidAt") OffsetDateTime paidAt, @Param("channel") String channel);

    /** Moves an order from one status to the next; 0 when it was no longer in {@code current}. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("UPDATE ProductOrder o SET o.status = :next, o.note = :note, o.transactionId = :txId "
            + "WHERE o.id = :id AND o.status = :current")
    int transition(@Param("id") Long id, @Param("current") Status current, @Param("next") Status next,
                   @Param("note") String note, @Param("txId") String transactionId);
}
