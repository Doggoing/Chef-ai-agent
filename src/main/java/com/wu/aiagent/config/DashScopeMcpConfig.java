package com.wu.aiagent.config;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashScopeMcpConfig {

    @Bean
    public McpSyncHttpClientRequestCustomizer dashScopeMcpAuthCustomizer(
            @Value("${spring.ai.dashscope.api-key}") String apiKey) {
        return (builder, method, endpoint, body, context) ->
                builder.header("Authorization", "Bearer " + apiKey);
    }
}
