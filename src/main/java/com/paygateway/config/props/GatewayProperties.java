package com.paygateway.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Core gateway tunables bound from the {@code gateway.*} namespace.
 */
@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties(
        BigDecimal feePercentage,
        BigDecimal feeFlat,
        Checkout checkout,
        Idempotency idempotency,
        Webhook webhook
) {
    public record Checkout(int sessionTtlMinutes) {
    }

    public record Idempotency(int ttlHours) {
    }

    public record Webhook(int maxAttempts, String baseUrl) {
    }
}
