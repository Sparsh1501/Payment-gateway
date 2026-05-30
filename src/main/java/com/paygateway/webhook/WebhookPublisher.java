package com.paygateway.webhook;

import java.util.UUID;

/**
 * Fans out a domain event to every active merchant webhook endpoint subscribed
 * to {@code eventType}. Implementations must not throw into the calling
 * (transactional) business flow.
 */
public interface WebhookPublisher {

    void publish(UUID merchantId, String eventType, Object payload);
}
