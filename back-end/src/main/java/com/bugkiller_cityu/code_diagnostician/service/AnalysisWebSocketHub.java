package com.bugkiller_cityu.code_diagnostician.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 projectId 管理 WebSocket 会话，并向订阅端推送分析事件。
 */
@Component
public class AnalysisWebSocketHub {

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
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            // 推送失败不影响分析主流程
            e.printStackTrace();
        }
    }
}
