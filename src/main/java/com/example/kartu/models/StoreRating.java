package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * SKPL-F21 (usulan): store satisfaction rating ("Nilai Kami"), visible publicly with the
 * admin's reply. Table: backend/docs/sql/002_store_ratings.sql and 005_rating_reply.sql
 * (one row per user, rating 1-5, comment up to 500 and reply up to 1000 characters).
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "store_ratings")
public class StoreRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Short rating;

    private String comment;

    // Admin reply, shown publicly under the review. Up to 1000 characters (docs/sql/005).
    @Column(name = "admin_reply", columnDefinition = "text")
    private String adminReply;

    @Column(name = "replied_at")
    private OffsetDateTime repliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Last change by the customer. Set by RatingService.submit, not on every save,
    // so an admin reply does not change the review's date or its place in the list.
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }
}
