package com.diagnostician.service.ai;

import com.diagnostician.entity.AiConversation;
import com.diagnostician.entity.AiMessage;
import com.diagnostician.exception.ResourceNotFoundException;
import com.diagnostician.repository.AiConversationRepository;
import com.diagnostician.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final String DEFAULT_TITLE = "新对话";

    private final ChatClient chatClient;
    private final EmbeddingService embeddingService;
    private final AiContextService contextService;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;

    public AiConversation createConversation(String projectId, String title) {
        contextService.requireProject(projectId);
        AiConversation conversation = new AiConversation();
        conversation.setProjectId(projectId);
        conversation.setTitle(title != null && !title.isBlank() ? title : DEFAULT_TITLE);
        conversation.setType("chat");
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        return conversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(String projectId, String conversationId) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("对话不存在"));
        if (!projectId.equals(conversation.getProjectId())) {
            throw new ResourceNotFoundException("对话不属于当前项目");
        }
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
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

    public List<Map<String, Object>> listMessages(String projectId, String conversationId) {
        AiConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("对话不存在"));
        if (!projectId.equals(conversation.getProjectId())) {
            throw new ResourceNotFoundException("对话不属于当前项目");
        }
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

        // 首条消息且标题仍为默认占位名时，待 AI 回复完成后自动生成贴切的对话标题
        boolean needsAutoTitle = DEFAULT_TITLE.equals(conversation.getTitle())
                && messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).isEmpty();

        saveMessage(conversationId, "user", userMessage, null);

        // RAG 检索：如果 embedding 可用则检索代码片段，否则降级为无检索对话
        String context;
        String references;
        if (embeddingService.isRagEnabled()) {
            if (!embeddingService.isIndexed(projectId)) {
                embeddingService.indexProject(projectId);
            }
            List<Document> docs = embeddingService.search(projectId, userMessage, 5);
            context = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));
            references = docs.stream()
                    .map(d -> String.valueOf(d.getMetadata().getOrDefault("filename", "unknown")))
                    .distinct()
                    .collect(Collectors.joining(", "));
        } else {
            // 降级模式：把项目文件列表作为上下文
            var files = contextService.listFiles(projectId);
            context = "项目文件列表（未启用 RAG，无法检索代码片段）：\n" +
                    files.stream().map(f -> "- " + f.getFilename()).collect(Collectors.joining("\n"));
            references = "";
        }

        String prompt = """
                你是代码库问答助手。请基于以下上下文回答用户问题。
                要求：
                1. 使用中文回答
                2. 引用具体文件（如果能推断）
                3. 如果上下文不足，明确说明需要查看哪些文件

                上下文：
                %s

                用户问题：%s
                """.formatted(context.isBlank() ? "（无可用上下文）" : context, userMessage);

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
                    if (needsAutoTitle) {
                        scheduleAutoTitle(conversation, userMessage);
                    }
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

    /**
     * 异步生成并更新对话标题，不阻塞流式响应的完成。
     * 生成失败时保留默认标题，不影响主流程。
     */
    private void scheduleAutoTitle(AiConversation conversation, String userMessage) {
        Mono.fromRunnable(() -> {
            try {
                String newTitle = generateTitle(userMessage);
                if (newTitle != null && !newTitle.isBlank()) {
                    conversation.setTitle(newTitle);
                    conversation.setUpdatedAt(Instant.now());
                    conversationRepository.save(conversation);
                }
            } catch (Exception ignored) {
                // 标题生成失败，保留默认标题
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    /**
     * 调用 AI 根据用户首条提问生成简洁的对话标题，并做清洗与长度截断。
     */
    private String generateTitle(String userMessage) {
        String prompt = """
                请为以下用户提问生成一个简洁的对话标题，要求：
                1. 不超过 20 个汉字
                2. 概括用户问题的核心主题
                3. 不要使用引号、书名号等包裹符号，不要以标点符号结尾
                4. 不要以"新对话"开头
                5. 只输出标题文本，不要任何额外解释或前缀

                用户提问：%s
                """.formatted(userMessage);
        String title = chatClient.prompt().user(prompt).call().content();
        if (title == null) {
            return null;
        }
        title = title.trim()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("^[\"'“”‘’《<]+", "")
                .replaceAll("[\"'“”‘’》。.！!？?；;，,>]+$", "")
                .trim();
        if (title.length() > 20) {
            title = title.substring(0, 20);
        }
        return title.isBlank() ? null : title;
    }
}
