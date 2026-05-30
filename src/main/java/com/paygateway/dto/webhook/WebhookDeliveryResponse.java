package com.paygateway.dto.webhook;

import com.paygateway.entity.WebhookDelivery;

import java.time.Instant;
import java.util.UUID;

public record WebhookDeliveryResponse(
        UUID id,
        UUID webhookEndpointId,
        String eventType,
        String status,
        int attempts,
        Instant nextRetryAt,
        Integer lastResponseCode,
        Instant createdAt
) {
    public static WebhookDeliveryResponse from(WebhookDelivery d) {
        return new WebhookDeliveryResponse(
                d.getId(), d.getWebhookEndpointId(), d.getEventType(),
                d.getStatus().name(), d.getAttempts(), d.getNextRetryAt(),
                d.getLastResponseCode(), d.getCreatedAt());
    }
}
