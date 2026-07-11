package com.diagnostician.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    /**
     * OpenRouter Embedding 模型：独立于 Chat 的 base-url 和 api-key。
     * 用于接入 OpenRouter 提供的 Embedding 模型（如 NVIDIA Llama Nemotron Embed VL 1B V2）。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.ai", name = "embedding-provider", havingValue = "openrouter")
    public EmbeddingModel openrouterEmbeddingModel(AiProperties aiProperties) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(aiProperties.getOpenrouterApiKey())
                .baseUrl(aiProperties.getOpenrouterBaseUrl())
                .build();

        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(aiProperties.getOpenrouterEmbeddingModel())
                        .build()
        );
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
