package com.diagnostician.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 projectId 管理 WebSocket 会话，并向订阅端推送分析事件。
 */
@Component
public class AnalysisWebSocketHub {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWebSocketHub.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<WebSocketSession>> sessionsByProject = new ConcurrentHashMap<>();

    public void register(String projectId, WebSocketSession session) {
        sessionsByProject
                .computeIfAbsent(projectId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void unregister(String projectId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByProject.get(projectId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByProject.remove(projectId);
            }
        }
    }

    public void send(String projectId, String type, Object payload) {
        Set<WebSocketSession> sessions = sessionsByProject.get(projectId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", type, "payload", payload));
            List<WebSocketSession> deadSessions = new ArrayList<>();
            for (WebSocketSession session : sessions) {
                if (!session.isOpen()) {
                    deadSessions.add(session);
                    continue;
                }
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    log.warn("WebSocket 推送失败: projectId={}, sessionId={}", projectId, session.getId(), e);
                    deadSessions.add(session);
                }
            }
            for (WebSocketSession dead : deadSessions) {
                unregister(projectId, dead);
            }
        } catch (IOException e) {
            log.warn("WebSocket 消息序列化失败: projectId={}, type={}", projectId, type, e);
        }
    }
}
