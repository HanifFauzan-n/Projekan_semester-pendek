package com.example.kartu.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** SKPL-F20 (usulan): one message of an AI chat session, role USER or AI. */
@Entity
@Data
@NoArgsConstructor
@Table(name = "ai_chat_messages")
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AiChatSession session;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AiChatMessage(AiChatSession session, String role, String content) {
        this.session = session;
        this.role = role;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
