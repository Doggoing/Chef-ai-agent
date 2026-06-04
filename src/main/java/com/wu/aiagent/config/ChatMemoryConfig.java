package com.wu.aiagent.config;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对话记忆配置：窗口 20 条，持久化到 PostgreSQL。
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * @param chatMemoryRepository PostgreSQL 记忆仓储
     * @return 窗口化对话记忆
     */
    @Bean
    MessageWindowChatMemory messageWindowChatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();
    }
}
