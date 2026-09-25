package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** SKPL-F20 (usulan): one AI chat conversation of an admin. Table: docs/sql/003_ai_chat.sql. */
@Entity
@Data
@NoArgsConstructor
@Table(name = "ai_chat_sessions")
public class AiChatSession {

    public static final String DEFAULT_TITLE = "Sesi baru";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User admin;

    @Column(nullable = false)
    private String title = DEFAULT_TITLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }
}
