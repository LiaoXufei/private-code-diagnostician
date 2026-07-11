package com.diagnostician.service.ai;

import com.diagnostician.entity.AiConversation;
import com.diagnostician.entity.AiMessage;
import com.diagnostician.exception.ResourceNotFoundException;
import com.diagnostician.repository.AiConversationRepository;
import com.diagnostician.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final AiContextService contextService;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    public AiConversation createConversation(String projectId, String title) {
        contextService.requireProject(projectId);
        AiConversation conversation = new AiConversation();
        conversation.setProjectId(projectId);
        conversation.setTitle(title != null && !title.isBlank() ? title : "代码对话");
        conversation.setType("chat");
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        return conversationRepository.save(conversation);
    }

    public List<Map<String, Object>> listConversations(String projectId) {
        return conversationRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("title", c.getTitle());
                    m.put("type", c.getType());
                    m.put("createdAt", c.getCreatedAt().toString());
                    m.put("updatedAt", c.getUpdatedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listMessages(String conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    map.put("createdAt", m.getCreatedAt().toString());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public Flux<String> streamChat(String projectId, String conversationId, String userMessage) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("对话不存在"));
        if (!projectId.equals(conversation.getProjectId())) {
            throw new ResourceNotFoundException("对话不属于当前项目");
        }

        if (!embeddingService.isIndexed(projectId)) {
            embeddingService.indexProject(projectId);
        }

        saveMessage(conversationId, "user", userMessage, null);

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userMessage)
                        .topK(5)
                        .similarityThreshold(0.45)
                        .filterExpression("projectId == '" + projectId + "'")
                        .build()
        );

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        String references = docs.stream()
                .map(d -> String.valueOf(d.getMetadata().getOrDefault("filename", "unknown")))
                .distinct()
                .collect(Collectors.joining(", "));

        String prompt = """
                你是代码库问答助手。请基于检索到的代码片段回答用户问题。
                要求：
                1. 使用中文回答
                2. 引用具体文件和行号（如果能推断）
                3. 如果上下文不足，明确说明需要查看哪些文件

                检索到的代码上下文：
                %s

                用户问题：%s
                """.formatted(context.isBlank() ? "（未检索到相关代码）" : context, userMessage);

        StringBuilder buffer = new StringBuilder();
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(buffer::append)
                .doOnComplete(() -> {
                    String finalContent = buffer.toString();
                    if (!references.isBlank()) {
                        finalContent += "\n\n引用文件: " + references;
                    }
                    saveMessage(conversationId, "assistant", finalContent, references);
                    conversation.setUpdatedAt(Instant.now());
                    conversationRepository.save(conversation);
                });
    }

    private void saveMessage(String conversationId, String role, String content, String references) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setReferencesJson(references);
        message.setCreatedAt(Instant.now());
        messageRepository.save(message);
    }
}
