package com.paygateway.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Redis sorted-set retry queue: member = delivery id, score = epoch-millis of
 * the next attempt. Fails open (logs and degrades) when Redis is unavailable —
 * a DB sweep in {@link WebhookDeliveryProcessor} acts as a safety net.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryQueue {

    private static final String KEY = "webhook:retry:queue";

    private final StringRedisTemplate redisTemplate;

    public void schedule(UUID deliveryId, Instant when) {
        try {
            redisTemplate.opsForZSet().add(KEY, deliveryId.toString(), when.toEpochMilli());
        } catch (Exception e) {
            log.warn("Redis unavailable scheduling webhook retry {}: {}", deliveryId, e.getMessage());
        }
    }

    public List<UUID> pollDue(int maxItems) {
        try {
            Set<String> due = redisTemplate.opsForZSet()
                    .rangeByScore(KEY, 0, Instant.now().toEpochMilli(), 0, maxItems);
            if (due == null || due.isEmpty()) {
                return Collections.emptyList();
            }
            return due.stream().map(UUID::fromString).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Redis unavailable polling webhook retry queue: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void remove(UUID deliveryId) {
        try {
            redisTemplate.opsForZSet().remove(KEY, deliveryId.toString());
        } catch (Exception e) {
            log.warn("Redis unavailable removing webhook retry {}: {}", deliveryId, e.getMessage());
        }
    }
}
