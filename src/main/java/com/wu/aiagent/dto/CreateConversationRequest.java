package com.wu.aiagent.dto;

import lombok.Data;

/**
 * 创建会话请求。
 */
@Data
public class CreateConversationRequest {

    private String scene = "chef_app";

    private String title;
}
