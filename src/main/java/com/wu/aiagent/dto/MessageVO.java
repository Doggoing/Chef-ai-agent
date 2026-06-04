package com.wu.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史消息项。
 */
@Data
@AllArgsConstructor
public class MessageVO {

    private String role;

    private String content;

    private LocalDateTime createdAt;
}
