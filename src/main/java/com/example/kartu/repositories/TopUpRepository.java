package com.example.kartu.repositories;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TopUp;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopUpRepository extends JpaRepository<TopUp, Integer> {
    // Untuk menampilkan riwayat di profile nanti
    List<TopUp> findByUserIdOrderByDateDesc(Integer userId);

    List<TopUp> findAllByOrderByDateDesc();

    // SKPL-F19: paged admin table
    org.springframework.data.domain.Page<TopUp> findAllByOrderByDateDesc(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<TopUp> findByStatusOrderByDateDesc(TransactionStatus status,
                                                                          org.springframework.data.domain.Pageable pageable);

    long countByStatusAndExternalIdIsNull(TransactionStatus status);

    Optional<TopUp> findByExternalId(String externalId);

    /**
     * Same lookup but locking the row, used by the webhook handler.
     * Xendit may deliver the same callback twice; without the lock two concurrent
     * callbacks can both read status PENDING and credit the balance twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TopUp t where t.externalId = :externalId")
    Optional<TopUp> findByExternalIdForUpdate(@Param("externalId") String externalId);

    /**
     * Moves a MANUAL top up (externalId null) out of PENDING in one statement.
     * Returns 0 when it was already processed or belongs to Xendit, so a double click on
     * "approve" can never credit twice and an unpaid Xendit invoice can never be approved.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TopUp t SET t.status = :next WHERE t.id = :id "
            + "AND t.status = com.example.kartu.enums.TransactionStatus.PENDING AND t.externalId IS NULL")
    int resolvePendingManual(@Param("id") Integer id, @Param("next") TransactionStatus next);

    /** Rows: status, method (MANUAL/XENDIT), count, total amount, for requests made in [from, to). */
    @Query("SELECT t.status, CASE WHEN t.externalId IS NULL THEN 'MANUAL' ELSE 'XENDIT' END, COUNT(t), COALESCE(SUM(t.amount), 0) "
            + "FROM TopUp t WHERE t.date >= :from AND t.date < :to "
            + "GROUP BY t.status, CASE WHEN t.externalId IS NULL THEN 'MANUAL' ELSE 'XENDIT' END")
    List<Object[]> summarize(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
