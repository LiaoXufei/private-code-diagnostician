package com.diagnostician.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** 是否启用 AI 功能 */
    private boolean enabled = true;

    /** 当前激活的模型提供商：openai / deepseek / qwen / ollama */
    private String activeProvider = "deepseek";

    /** SimpleVectorStore 持久化目录 */
    private String vectorStoreDir = "data/vectorstore";

    /** 优化建议时最多纳入的 Issue 数量 */
    private int maxContextIssues = 20;

    /** 单次请求最多纳入的源码字符数 */
    private int maxCodeChars = 8000;
}
