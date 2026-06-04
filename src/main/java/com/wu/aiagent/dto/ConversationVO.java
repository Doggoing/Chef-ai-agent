package com.wu.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项。
 */
@Data
@AllArgsConstructor
public class ConversationVO {

    private String conversationId;

    private String title;

    private String scene;

    private Integer messageCount;

    private LocalDateTime lastMessageAt;
}
