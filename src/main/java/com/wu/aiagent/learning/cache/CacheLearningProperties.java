package com.wu.aiagent.learning.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存学习模块配置项。
 */
@Data
@ConfigurationProperties(prefix = "cache-learning")
public class CacheLearningProperties {

    private final Caffeine caffeine = new Caffeine();
    private final Redis redis = new Redis();

    @Data
    public static class Caffeine {
        private long maxSize = 500;
        private long expireSeconds = 60;
    }

    @Data
    public static class Redis {
        private long ttlSeconds = 1800;
    }
}
