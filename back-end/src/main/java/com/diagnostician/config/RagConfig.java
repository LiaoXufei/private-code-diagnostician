package com.diagnostician.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Slf4j
@Configuration
public class RagConfig {

    /**
     * 当 embedding-provider 不为 none 时创建 VectorStore。
     * 支持 openrouter / openai / ollama 三种 Embedding 来源。
     */
    @Bean
    @ConditionalOnExpression("!'none'.equalsIgnoreCase('${app.ai.embedding-provider:none}')")
    public VectorStore vectorStore(EmbeddingModel embeddingModel, AiProperties aiProperties) {
        File dir = new File(aiProperties.getVectorStoreDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        // 启动时加载已持久化的向量，避免每次重启都要重新索引
        File persistFile = new File(dir, "vectorstore.json");
        if (persistFile.exists()) {
            try {
                store.load(persistFile);
            } catch (Exception e) {
                log.warn("向量库持久化文件加载失败，将重新索引: {}", e.getMessage());
            }
        }
        return store;
    }
}
