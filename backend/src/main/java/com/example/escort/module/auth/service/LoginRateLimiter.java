package com.example.escort.module.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流（单机内存实现，BUG-029）。
 *
 * 规则：同一 IP + 账号在窗口期内连续失败达到阈值后锁定；锁定期间即使密码正确也拒绝。
 * 登录成功清零；窗口过期/锁定期满后自动恢复。定时清理空闲桶，避免内存泄漏。
 *
 * 注意：这是单节点内存实现；多实例部署时各节点独立计数。若需要全局一致，
 * 应替换为 Redis/数据库实现，而不是在本类中硬编码测试绕过。
 */
@Slf4j
@Component
public class LoginRateLimiter {

    private final int maxFailures;
    private final int windowMinutes;
    private final int lockMinutes;
    private final Clock clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public LoginRateLimiter(@Value("${security.login.max-failures:5}") int maxFailures,
                            @Value("${security.login.window-minutes:5}") int windowMinutes,
                            @Value("${security.login.lock-minutes:15}") int lockMinutes,
                            Clock clock) {
        this.maxFailures = Math.max(1, maxFailures);
        this.windowMinutes = Math.max(1, windowMinutes);
        this.lockMinutes = Math.max(1, lockMinutes);
        this.clock = clock;
    }

    public String key(String ip, String username) {
        return (ip == null ? "unknown" : ip) + "|" + (username == null ? "" : username.trim());
    }

    /**
     * @return 锁定剩余毫秒；未锁定返回 0
     */
    public long checkLocked(String ip, String username) {
        Bucket bucket = buckets.get(key(ip, username));
        if (bucket == null) return 0;
        long now = clock.millis();
        synchronized (bucket) {
            if (bucket.lockUntil > now) return bucket.lockUntil - now;
            if (bucket.lockUntil > 0) {
                bucket.lockUntil = 0;
                bucket.failures.clear();
            }
            purgeExpired(bucket, now);
            return 0;
        }
    }

    public void recordFailure(String ip, String username) {
        String key = key(ip, username);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        long now = clock.millis();
        synchronized (bucket) {
            if (bucket.lockUntil > now) return;
            if (bucket.lockUntil > 0) {
                bucket.lockUntil = 0;
                bucket.failures.clear();
            }
            purgeExpired(bucket, now);
            bucket.failures.addLast(now);
            if (bucket.failures.size() >= maxFailures) {
                bucket.lockUntil = now + lockMinutes * 60_000L;
                bucket.failures.clear();
                log.warn("登录失败达到阈值，锁定 {} 分钟: key={}", lockMinutes, key);
            }
        }
    }

    public void reset(String ip, String username) {
        buckets.remove(key(ip, username));
    }

    @Scheduled(fixedDelayString = "${security.login.cleanup-interval-ms:60000}")
    public void cleanup() {
        long now = clock.millis();
        Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bucket> entry = it.next();
            Bucket bucket = entry.getValue();
            synchronized (bucket) {
                if (bucket.lockUntil > now) continue;
                if (bucket.lockUntil > 0) {
                    bucket.lockUntil = 0;
                }
                purgeExpired(bucket, now);
                if (bucket.failures.isEmpty()) it.remove();
            }
        }
    }

    private void purgeExpired(Bucket bucket, long now) {
        long windowStart = now - windowMinutes * 60_000L;
        while (!bucket.failures.isEmpty() && bucket.failures.peekFirst() < windowStart) {
            bucket.failures.pollFirst();
        }
    }

    private static class Bucket {
        private final Deque<Long> failures = new ArrayDeque<>();
        private long lockUntil;
    }
}
