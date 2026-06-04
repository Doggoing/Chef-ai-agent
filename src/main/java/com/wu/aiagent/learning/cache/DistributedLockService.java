package com.wu.aiagent.learning.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的分布式锁示例：防止多实例并发扣减同一商品库存。
 */
@Slf4j
@Service
@Profile("cache-learning")
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String LOCK_PREFIX = "learning:lock:stock:";

    private final RedissonClient redissonClient;
    private final Map<Long, Integer> stockTable = new ConcurrentHashMap<>();

    /**
     * 在分布式锁保护下扣减库存。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 扣减结果
     */
    public StockDeductResult deductStock(Long productId, int quantity) {
        String lockKey = LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                return new StockDeductResult(productId, false, currentStock(productId), "获取分布式锁失败");
            }
            int current = currentStock(productId);
            if (current < quantity) {
                return new StockDeductResult(productId, false, current, "库存不足");
            }
            stockTable.put(productId, current - quantity);
            log.info("扣减成功 productId={}, quantity={}, remain={}", productId, quantity, current - quantity);
            return new StockDeductResult(productId, true, current - quantity, "扣减成功");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new StockDeductResult(productId, false, currentStock(productId), "线程被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询当前库存（演示用，初始 100）。
     *
     * @param productId 商品 ID
     * @return 库存数量
     */
    public int currentStock(Long productId) {
        return stockTable.computeIfAbsent(productId, id -> 100);
    }

    /**
     * 扣减结果。
     *
     * @param productId 商品 ID
     * @param success   是否成功
     * @param stock     操作后库存
     * @param message   说明
     */
    public record StockDeductResult(Long productId, boolean success, int stock, String message) {
    }
}
