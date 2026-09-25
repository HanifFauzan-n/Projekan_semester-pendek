package com.example.kartu.repositories;

import com.example.kartu.models.AiChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AiChatMessage m WHERE m.session.id = :sessionId")
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    /** Newest first; the caller reverses it to build the model context. */
    @Query("SELECT m FROM AiChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt DESC, m.id DESC")
    List<AiChatMessage> findRecent(@Param("sessionId") Long sessionId, Pageable limit);
}
