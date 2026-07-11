package com.diagnostician.config;

import com.diagnostician.service.ai.tools.CodeAnalysisTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将 @Tool 分析能力注册为 MCP Server 工具，供 IDE Agent 复用。
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(CodeAnalysisTools codeAnalysisTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(codeAnalysisTools)
                .build();
    }
}
