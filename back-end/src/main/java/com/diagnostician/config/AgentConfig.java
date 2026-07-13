package com.diagnostician.config;

import com.diagnostician.service.ai.tools.CodeAnalysisTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public ChatClient agentChatClient(ChatModel chatModel, CodeAnalysisTools codeAnalysisTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是 Code-Diagnostician 的 AI 代码质量诊断 Agent。
                        你可以调用工具读取项目文件、运行静态分析指标、查询分析结果。
                        请按以下步骤工作：
                        1. 了解项目基本信息和文件列表
                        2. 运行静态分析或读取已有分析结果
                        3. 总结主要质量问题并给出优先级修复建议
                        回答使用中文，引用文件时注明路径和行号。
                        """)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultTools(codeAnalysisTools)
                .build();
    }
}
