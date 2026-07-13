package com.diagnostician.websocket;

import com.diagnostician.repository.ProjectRepository;
import com.diagnostician.service.AnalysisWebSocketHub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AnalysisWebSocketHandler extends TextWebSocketHandler {

    private static final UriTemplate PATH_TEMPLATE = new UriTemplate("/ws/analysis/{projectId}");

    private final AnalysisWebSocketHub hub;
    private final ProjectRepository projectRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String projectId = extractProjectId(session);
        if (projectId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        if (!projectRepository.existsById(projectId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("项目不存在"));
            return;
        }
        hub.register(projectId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 前端心跳 ping，无需业务处理
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String projectId = extractProjectId(session);
        if (projectId != null) {
            hub.unregister(projectId, session);
        }
    }

    private String extractProjectId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        Map<String, String> vars = PATH_TEMPLATE.match(session.getUri().getPath());
        return vars.get("projectId");
    }
}
