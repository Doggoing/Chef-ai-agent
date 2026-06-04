package com.wu.aiagent.learning.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多级缓存示例：L1 Caffeine → L2 Redis → 模拟数据库。
 */
@Slf4j
@Service
@Profile("cache-learning")
@RequiredArgsConstructor
public class MultiLevelCacheService {

    private static final String KEY_PREFIX = "learning:product:";

    private final Cache<String, String> productLocalCache;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheLearningProperties properties;

    private final Map<Long, String> fakeDatabase = new ConcurrentHashMap<>();
    private final AtomicLong dbQueryCount = new AtomicLong();

    /**
     * 按商品 ID 查询，演示三级回源与命中统计。
     *
     * @param productId 商品 ID
     * @return 查询结果（含命中层级说明）
     */
    public CacheHitResult getProduct(Long productId) {
        String cacheKey = KEY_PREFIX + productId;

        String localValue = productLocalCache.getIfPresent(cacheKey);
        if (localValue != null) {
            return new CacheHitResult(productId, localValue, "L1-Caffeine", dbQueryCount.get());
        }

        String redisValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (redisValue != null) {
            productLocalCache.put(cacheKey, redisValue);
            return new CacheHitResult(productId, redisValue, "L2-Redis", dbQueryCount.get());
        }

        String dbValue = loadFromDatabase(productId);
        Duration redisTtl = Duration.ofSeconds(properties.getRedis().getTtlSeconds());
        stringRedisTemplate.opsForValue().set(cacheKey, dbValue, redisTtl);
        productLocalCache.put(cacheKey, dbValue);

        return new CacheHitResult(productId, dbValue, "DB", dbQueryCount.get());
    }

    /**
     * 使指定商品缓存失效（常用于更新后删缓存）。
     *
     * @param productId 商品 ID
     */
    public void evictProduct(Long productId) {
        String cacheKey = KEY_PREFIX + productId;
        productLocalCache.invalidate(cacheKey);
        stringRedisTemplate.delete(cacheKey);
    }

    /**
     * 返回 Caffeine 统计，便于观察 L1 命中率。
     *
     * @return 统计摘要
     */
    public String caffeineStats() {
        return productLocalCache.stats().toString();
    }

    private String loadFromDatabase(Long productId) {
        dbQueryCount.incrementAndGet();
        return fakeDatabase.computeIfAbsent(productId, id -> {
            log.info("模拟查库 productId={}", id);
            return "商品-" + id + "-详情-来自DB";
        });
    }

    /**
     * 单次查询结果。
     *
     * @param productId   商品 ID
     * @param value       数据
     * @param hitLevel    命中层级
     * @param dbQueryCount 累计查库次数
     */
    public record CacheHitResult(Long productId, String value, String hitLevel, long dbQueryCount) {
    }
}
