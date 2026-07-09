package com.bugkiller_cityu.code_diagnostician.config;

import com.bugkiller_cityu.code_diagnostician.websocket.AnalysisWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AnalysisWebSocketHandler analysisWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 前端期望：ws://host/ws/analysis/{projectId}
        registry.addHandler(analysisWebSocketHandler, "/ws/analysis/{projectId}")
                .setAllowedOrigins("*");
    }
}
