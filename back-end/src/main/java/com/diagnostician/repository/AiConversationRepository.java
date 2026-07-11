package com.diagnostician.repository;

import com.diagnostician.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiConversationRepository extends JpaRepository<AiConversation, String> {
    List<AiConversation> findByProjectIdOrderByUpdatedAtDesc(String projectId);
}
