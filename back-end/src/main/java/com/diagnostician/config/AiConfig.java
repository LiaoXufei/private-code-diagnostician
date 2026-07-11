package com.diagnostician.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    @Bean
    @Primary
    public ChatModel primaryChatModel(
            AiProperties aiProperties,
            ObjectProvider<OpenAiChatModel> openAiChatModel,
            ObjectProvider<OllamaChatModel> ollamaChatModel
    ) {
        if ("ollama".equalsIgnoreCase(aiProperties.getActiveProvider())) {
            return ollamaChatModel.getObject();
        }
        return openAiChatModel.getObject();
    }

    @Bean
    @Primary
    public EmbeddingModel primaryEmbeddingModel(
            AiProperties aiProperties,
            ObjectProvider<OpenAiEmbeddingModel> openAiEmbeddingModel,
            ObjectProvider<OllamaEmbeddingModel> ollamaEmbeddingModel
    ) {
        if ("ollama".equalsIgnoreCase(aiProperties.getActiveProvider())) {
            return ollamaEmbeddingModel.getObject();
        }
        return openAiEmbeddingModel.getObject();
    }

    @Bean
    public MessageWindowChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(30)
                .build();
    }

    @Bean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor());
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, MessageWindowChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
