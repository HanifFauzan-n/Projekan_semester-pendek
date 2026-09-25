package com.example.kartu.repositories;

import com.example.kartu.models.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiChatRepository extends JpaRepository<AiChatSession, Long> {

    List<AiChatSession> findByAdminIdOrderByUpdatedAtDesc(Integer adminId);

    Optional<AiChatSession> findByIdAndAdminId(Long id, Integer adminId);
}
