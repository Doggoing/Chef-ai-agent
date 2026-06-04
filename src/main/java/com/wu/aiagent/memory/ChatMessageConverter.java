package com.wu.aiagent.memory;

import com.wu.aiagent.entity.ChatMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI Message 与数据库实体互转。
 */
public final class ChatMessageConverter {

    private ChatMessageConverter() {
    }

    /**
     * 将 Spring AI 消息转为持久化实体。
     *
     * @param conversationId 会话 ID
     * @param message        AI 消息
     * @return 数据库实体
     */
    public static ChatMessage toEntity(String conversationId, Message message) {
        ChatMessage entity = new ChatMessage();
        entity.setConversationId(conversationId);
        entity.setRole(message.getMessageType().getValue());
        entity.setContent(message.getText());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * 将数据库实体转为 Spring AI 消息。
     *
     * @param entity 数据库实体
     * @return AI 消息
     */
    public static Message toSpringMessage(ChatMessage entity) {
        MessageType type = MessageType.fromValue(entity.getRole());
        return switch (type) {
            case USER -> new UserMessage(entity.getContent());
            case ASSISTANT -> new AssistantMessage(entity.getContent());
            case SYSTEM -> new SystemMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
    }

    /**
     * 批量转为 Spring AI 消息列表。
     *
     * @param entities 数据库实体列表
     * @return AI 消息列表
     */
    public static List<Message> toSpringMessages(List<ChatMessage> entities) {
        List<Message> messages = new ArrayList<>(entities.size());
        for (ChatMessage entity : entities) {
            messages.add(toSpringMessage(entity));
        }
        return messages;
    }
}
