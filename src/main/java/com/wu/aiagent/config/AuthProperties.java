package com.wu.aiagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 认证配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "auth.jwt")
public class AuthProperties {

    private String secret = "wu-ai-agent-dev-secret-change-me";

    private int expireHours = 168;
}
