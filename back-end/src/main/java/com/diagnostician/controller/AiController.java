package com.diagnostician.controller;

import com.diagnostician.config.AiProperties;
import com.diagnostician.entity.AiConversation;
import com.diagnostician.exception.AiServiceException;
import com.diagnostician.service.ai.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@CrossOrigin
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiController {

    private final AiProperties aiProperties;
    private final AiOptimizeService optimizeService;
    private final AiChatService chatService;
    private final AgentService agentService;
    private final EmbeddingService embeddingService;
    private final AtomicLong aiRequestCounter;

    @GetMapping("/ai/status")
    public Map<String, Object> aiStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", aiProperties.isEnabled());
        status.put("activeProvider", aiProperties.getActiveProvider());
        status.put("features", List.of("optimize", "chat", "rag", "agent", "mcp"));
        status.put("mcpEndpoint", "/mcp");
        status.put("totalRequests", aiRequestCounter.get());
        return status;
    }

    @GetMapping("/ai/models")
    public Map<String, Object> listModels() {
        return Map.of(
                "activeProvider", aiProperties.getActiveProvider(),
                "providers", List.of("openai", "deepseek", "qwen", "ollama"),
                "note", "通过环境变量 AI_PROVIDER 与 OPENAI_BASE_URL 切换模型"
        );
    }

    @PostMapping(value = "/projects/{id}/ai/summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSummary(@PathVariable String id) {
        return streamResponse(optimizeService.streamSummary(id));
    }

    @PostMapping(value = "/projects/{id}/ai/optimize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOptimize(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String issueId = body != null ? body.get("issueId") : null;
        if (issueId != null && !issueId.isBlank()) {
            return streamResponse(optimizeService.streamOptimize(id, issueId));
        }
        return streamResponse(optimizeService.streamBatchOptimize(id));
    }

    @PostMapping(value = "/projects/{id}/ai/fix/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFix(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String issueId = body.get("issueId");
        if (issueId == null || issueId.isBlank()) {
            throw new AiServiceException("issueId 不能为空");
        }
        return streamResponse(optimizeService.streamFixCode(id, issueId));
    }

    @PostMapping("/projects/{id}/ai/embeddings")
    public Map<String, Object> indexEmbeddings(@PathVariable String id) {
        ensureAiEnabled();
        return embeddingService.indexProject(id);
    }

    @GetMapping("/projects/{id}/ai/conversations")
    public List<Map<String, Object>> listConversations(@PathVariable String id) {
        return chatService.listConversations(id);
    }

    @PostMapping("/projects/{id}/ai/conversations")
    public Map<String, Object> createConversation(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String title = body != null ? body.get("title") : null;
        AiConversation conversation = chatService.createConversation(id, title);
        return Map.of(
                "id", conversation.getId(),
                "title", conversation.getTitle(),
                "projectId", conversation.getProjectId()
        );
    }

    @GetMapping("/projects/{id}/ai/conversations/{conversationId}/messages")
    public List<Map<String, Object>> listMessages(
            @PathVariable String id,
            @PathVariable String conversationId
    ) {
        return chatService.listMessages(conversationId);
    }

    @PostMapping(value = "/projects/{id}/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String conversationId = body.get("conversationId");
        String message = body.get("message");
        if (conversationId == null || message == null || message.isBlank()) {
            throw new AiServiceException("conversationId 和 message 不能为空");
        }
        return streamResponse(chatService.streamChat(id, conversationId, message));
    }

    @PostMapping(value = "/projects/{id}/ai/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgent(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String prompt = body != null ? body.get("prompt") : null;
        return streamResponse(agentService.streamDiagnosis(id, prompt));
    }

    private SseEmitter streamResponse(reactor.core.publisher.Flux<String> flux) {
        ensureAiEnabled();
        aiRequestCounter.incrementAndGet();
        SseEmitter emitter = new SseEmitter(300_000L);
        flux.publishOn(Schedulers.boundedElastic())
                .subscribe(
                        token -> {
                            try {
                                emitter.send(SseEmitter.event().data(token));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
    }

    private void ensureAiEnabled() {
        if (!aiProperties.isEnabled()) {
            throw new AiServiceException("AI 功能未启用，请在配置中设置 app.ai.enabled=true");
        }
    }
}
