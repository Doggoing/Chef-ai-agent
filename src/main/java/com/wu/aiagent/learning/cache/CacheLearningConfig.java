package com.wu.aiagent.learning.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 仅在 profile {@code cache-learning} 下注册 L1 Caffeine 与 Redis 模板。
 */
@Configuration
@Profile("cache-learning")
@EnableConfigurationProperties(CacheLearningProperties.class)
public class CacheLearningConfig {

    /**
     * L1 本地缓存：进程内、命中最快，容量与 TTL 较小。
     *
     * @param properties 缓存学习配置
     * @return Caffeine 缓存实例
     */
    @Bean
    public Cache<String, String> productLocalCache(CacheLearningProperties properties) {
        CacheLearningProperties.Caffeine cfg = properties.getCaffeine();
        return Caffeine.newBuilder()
                .maximumSize(cfg.getMaxSize())
                .expireAfterWrite(cfg.getExpireSeconds(), TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    /**
     * L2 Redis：多实例共享，TTL 通常长于本地缓存。
     *
     * @param connectionFactory Redis 连接工厂
     * @return 字符串 Redis 模板
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
