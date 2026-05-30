package com.paygateway.dto.webhook;

import com.paygateway.entity.WebhookEndpoint;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record WebhookEndpointResponse(
        UUID id,
        String url,
        String secret,
        List<String> events,
        boolean active,
        Instant createdAt
) {
    public static WebhookEndpointResponse from(WebhookEndpoint e) {
        return new WebhookEndpointResponse(
                e.getId(), e.getUrl(), e.getSecret(),
                e.getEvents() == null ? List.of() : Arrays.asList(e.getEvents()),
                e.isActive(), e.getCreatedAt());
    }
}
