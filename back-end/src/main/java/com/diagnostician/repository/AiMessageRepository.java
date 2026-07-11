package com.diagnostician.repository;

import com.diagnostician.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, String> {
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
