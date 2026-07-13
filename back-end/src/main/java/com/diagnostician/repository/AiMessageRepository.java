package com.diagnostician.repository;

import com.diagnostician.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, String> {
    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    @Modifying
    @Transactional
    @Query("delete from AiMessage m where m.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") String conversationId);
}
