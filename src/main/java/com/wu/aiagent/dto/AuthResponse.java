package com.wu.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录 / 注册响应。
 */
@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private Long userId;

    private String username;
}
