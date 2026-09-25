package com.example.kartu.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.kartu.enums.TransactionStatus;
import com.example.kartu.models.TransactionHistory;

/**
 * Report queries aggregate in the database (JPQL, CLAUDE.md rule 9) over successful sales
 * in the half-open range [from, to). They serve the admin dashboard and the AI chat.
 * Cost falls back to 90% of the paid amount for rows saved before cost_price existed,
 * the same rule the dashboard used before.
 */
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, Integer> {
    List<TransactionHistory> findByUserId(Integer integer);

    List<TransactionHistory> findAllByOrderByTimestampDesc();

    // Mengambil transaksi user tertentu, urut dari yang paling baru
    List<TransactionHistory> findByUserIdOrderByTimestampDesc(Integer userId);

    boolean existsByTransactionId(String newId);

    boolean existsBySerialNumber(String sn);

    long countByUserIdAndStatus(Integer userId, TransactionStatus status);

    String SUCCESS_IN_RANGE = " t.status = com.example.kartu.enums.TransactionStatus.SUCCESS"
            + " AND t.timestamp >= :from AND t.timestamp < :to ";

    /** One row: count, revenue, cost, discount, distinct customers. */
    @Query("SELECT COUNT(t), COALESCE(SUM(t.amountPaid), 0), "
            + "COALESCE(SUM(COALESCE(t.costPrice, t.amountPaid * 0.9)), 0), "
            + "COALESCE(SUM(t.discountAmount), 0), COUNT(DISTINCT t.user.id) "
            + "FROM TransactionHistory t WHERE" + SUCCESS_IN_RANGE)
    List<Object[]> summarize(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Rows: date, count, revenue. */
    @Query("SELECT CAST(t.timestamp AS LocalDate), COUNT(t), COALESCE(SUM(t.amountPaid), 0) "
            + "FROM TransactionHistory t WHERE" + SUCCESS_IN_RANGE
            + "GROUP BY CAST(t.timestamp AS LocalDate) ORDER BY CAST(t.timestamp AS LocalDate)")
    List<Object[]> dailySales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Rows: year, month, count, revenue. */
    @Query("SELECT YEAR(t.timestamp), MONTH(t.timestamp), COUNT(t), COALESCE(SUM(t.amountPaid), 0) "
            + "FROM TransactionHistory t WHERE" + SUCCESS_IN_RANGE
            + "GROUP BY YEAR(t.timestamp), MONTH(t.timestamp) ORDER BY YEAR(t.timestamp), MONTH(t.timestamp)")
    List<Object[]> monthlySales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Rows: product name, count, revenue. Limit through the Pageable. */
    @Query("SELECT p.name, COUNT(t), COALESCE(SUM(t.amountPaid), 0) "
            + "FROM TransactionHistory t JOIN t.product p WHERE" + SUCCESS_IN_RANGE
            + "GROUP BY p.id, p.name ORDER BY COUNT(t) DESC, SUM(t.amountPaid) DESC")
    List<Object[]> topProducts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable limit);

    /** Rows: category type, count, revenue. */
    @Query("SELECT COALESCE(c.type, '-'), COUNT(t), COALESCE(SUM(t.amountPaid), 0) "
            + "FROM TransactionHistory t LEFT JOIN t.product p LEFT JOIN p.category c WHERE" + SUCCESS_IN_RANGE
            + "GROUP BY c.type ORDER BY COUNT(t) DESC")
    List<Object[]> salesByCategory(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Rows: provider name, count, revenue. */
    @Query("SELECT COALESCE(pr.name, '-'), COUNT(t), COALESCE(SUM(t.amountPaid), 0) "
            + "FROM TransactionHistory t LEFT JOIN t.product p LEFT JOIN p.provider pr WHERE" + SUCCESS_IN_RANGE
            + "GROUP BY pr.name ORDER BY COUNT(t) DESC")
    List<Object[]> salesByProvider(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Sales report (SKPL-F16/F17/F19). :q is a lower-case LIKE pattern, "%" for no search.
    String SALES_SEARCH = " WHERE" + SUCCESS_IN_RANGE + "AND (LOWER(t.transactionId) LIKE :q "
            + "OR LOWER(COALESCE(t.customer, '')) LIKE :q OR COALESCE(t.customerNumber, '') LIKE :q "
            + "OR LOWER(COALESCE(p.name, '')) LIKE :q) ";

    @Query(value = "SELECT t FROM TransactionHistory t LEFT JOIN FETCH t.product p" + SALES_SEARCH + "ORDER BY t.timestamp DESC",
            countQuery = "SELECT COUNT(t) FROM TransactionHistory t LEFT JOIN t.product p" + SALES_SEARCH)
    Page<TransactionHistory> searchSales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                         @Param("q") String q, Pageable pageable);

    /** Same rows oldest first, for PDF/Excel export. */
    @Query("SELECT t FROM TransactionHistory t LEFT JOIN FETCH t.product p" + SALES_SEARCH + "ORDER BY t.timestamp ASC")
    List<TransactionHistory> searchSalesForExport(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                                  @Param("q") String q);

    /** One row over the same filter: count, revenue, cost, discount. */
    @Query("SELECT COUNT(t), COALESCE(SUM(t.amountPaid), 0), "
            + "COALESCE(SUM(COALESCE(t.costPrice, t.amountPaid * 0.9)), 0), COALESCE(SUM(t.discountAmount), 0) "
            + "FROM TransactionHistory t LEFT JOIN t.product p" + SALES_SEARCH)
    List<Object[]> summarizeSales(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("q") String q);

    /** Rows: voucher code, count, total discount, revenue. */
    @Query("SELECT t.voucherCode, COUNT(t), COALESCE(SUM(t.discountAmount), 0), COALESCE(SUM(t.amountPaid), 0) "
            + "FROM TransactionHistory t WHERE" + SUCCESS_IN_RANGE + "AND t.voucherCode IS NOT NULL "
            + "GROUP BY t.voucherCode ORDER BY COUNT(t) DESC")
    List<Object[]> voucherUsage(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
