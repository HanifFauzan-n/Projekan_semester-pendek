package com.example.kartu.repositories;

import com.example.kartu.models.StoreRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface StoreRatingRepository extends JpaRepository<StoreRating, Long> {

    Optional<StoreRating> findByUserId(Integer userId);

    /** One row: average rating, count, for ratings given or updated in [from, to). */
    @Query("SELECT COALESCE(AVG(r.rating), 0), COUNT(r) FROM StoreRating r "
            + "WHERE r.updatedAt >= :from AND r.updatedAt < :to")
    List<Object[]> summarize(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /** Rows: star value, count. */
    @Query("SELECT r.rating, COUNT(r) FROM StoreRating r "
            + "WHERE r.updatedAt >= :from AND r.updatedAt < :to GROUP BY r.rating")
    List<Object[]> distribution(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @EntityGraph(attributePaths = "user")
    Page<StoreRating> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<StoreRating> findByAdminReplyIsNullOrderByUpdatedAtDesc(Pageable pageable);

    long countByAdminReplyIsNull();

    List<StoreRating> findTop20ByCommentIsNotNullAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(
            OffsetDateTime from, OffsetDateTime to);
}
