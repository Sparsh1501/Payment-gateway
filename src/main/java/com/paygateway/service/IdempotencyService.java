package com.paygateway.service;

import com.paygateway.config.props.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed idempotency cache keyed by {@code idempotency:{merchantId}:{key}}.
 *
 * <p>Fails open: if Redis is unavailable, idempotency checks are skipped and a
 * warning is logged rather than failing the request.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final GatewayProperties gatewayProperties;

    private String key(UUID merchantId, String idempotencyKey) {
        return "idempotency:" + merchantId + ":" + idempotencyKey;
    }

    public Optional<String> lookup(UUID merchantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key(merchantId, idempotencyKey)));
        } catch (Exception e) {
            log.warn("Redis unavailable during idempotency lookup, failing open: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void store(UUID merchantId, String idempotencyKey, String responseJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    key(merchantId, idempotencyKey),
                    responseJson,
                    Duration.ofHours(gatewayProperties.idempotency().ttlHours()));
        } catch (Exception e) {
            log.warn("Redis unavailable during idempotency store, skipping cache: {}", e.getMessage());
        }
    }
}
