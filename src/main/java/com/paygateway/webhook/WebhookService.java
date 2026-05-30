package com.paygateway.webhook;

import com.paygateway.config.props.GatewayProperties;
import com.paygateway.entity.WebhookDelivery;
import com.paygateway.entity.WebhookEndpoint;
import com.paygateway.entity.enums.DeliveryStatus;
import com.paygateway.metrics.PaymentMetrics;
import com.paygateway.repository.WebhookDeliveryRepository;
import com.paygateway.repository.WebhookEndpointRepository;
import com.paygateway.util.HmacUtil;
import com.paygateway.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService implements WebhookPublisher {

    /** Backoff schedule between attempts: 1min, 5min, 30min, 2hr, 24hr. */
    private static final Duration[] BACKOFF = {
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(24)
    };

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookRetryQueue retryQueue;
    private final WebClient webhookWebClient;
    private final JsonUtil jsonUtil;
    private final GatewayProperties gatewayProperties;
    private final PaymentMetrics metrics;

    @Override
    @Transactional
    public void publish(UUID merchantId, String eventType, Object payload) {
        try {
            List<WebhookEndpoint> endpoints = endpointRepository.findByMerchantIdAndActiveTrue(merchantId);
            for (WebhookEndpoint endpoint : endpoints) {
                if (!subscribed(endpoint, eventType)) {
                    continue;
                }
                Map<String, Object> envelope = new LinkedHashMap<>();
                envelope.put("event", eventType);
                envelope.put("created", Instant.now().toString());
                envelope.put("data", payload);
                String body = jsonUtil.toJson(envelope);

                WebhookDelivery delivery = WebhookDelivery.builder()
                        .webhookEndpointId(endpoint.getId())
                        .eventType(eventType)
                        .payload(body)
                        .status(DeliveryStatus.PENDING)
                        .attempts(0)
                        .nextRetryAt(Instant.now())
                        .build();
                delivery = deliveryRepository.save(delivery);
                retryQueue.schedule(delivery.getId(), Instant.now());
                log.info("Queued webhook delivery {} event={} endpoint={}",
                        delivery.getId(), eventType, endpoint.getId());
            }
        } catch (Exception e) {
            // Never break the calling business transaction because of webhook fan-out.
            log.error("Failed to enqueue webhooks for merchant {} event {}: {}",
                    merchantId, eventType, e.getMessage(), e);
        }
    }

    private boolean subscribed(WebhookEndpoint endpoint, String eventType) {
        if (endpoint.getEvents() == null) {
            return false;
        }
        for (String e : endpoint.getEvents()) {
            if (e.equals(eventType) || e.equals("*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempt a single delivery and update its persisted state + retry schedule.
     */
    @Transactional
    public void attemptDelivery(UUID deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null || delivery.getStatus() == DeliveryStatus.SUCCESS) {
            retryQueue.remove(deliveryId);
            return;
        }
        WebhookEndpoint endpoint = endpointRepository.findById(delivery.getWebhookEndpointId()).orElse(null);
        if (endpoint == null) {
            delivery.setStatus(DeliveryStatus.FAILED);
            deliveryRepository.save(delivery);
            retryQueue.remove(deliveryId);
            return;
        }

        int attempt = delivery.getAttempts() + 1;
        delivery.setAttempts(attempt);
        String signature = HmacUtil.hmacSha256Hex(endpoint.getSecret(), delivery.getPayload());

        try {
            var response = webhookWebClient.post()
                    .uri(endpoint.getUrl())
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Event", delivery.getEventType())
                    .header("X-Webhook-Delivery", delivery.getId().toString())
                    .bodyValue(delivery.getPayload())
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(12));

            int code = response != null ? response.getStatusCode().value() : 0;
            delivery.setLastResponseCode(code);
            delivery.setStatus(DeliveryStatus.SUCCESS);
            delivery.setNextRetryAt(null);
            metrics.recordWebhookAttempt("success");
            log.info("Webhook delivery {} succeeded (HTTP {}) on attempt {}", deliveryId, code, attempt);
            retryQueue.remove(deliveryId);
        } catch (WebClientResponseException e) {
            delivery.setLastResponseCode(e.getStatusCode().value());
            handleFailure(delivery, attempt);
        } catch (Exception e) {
            delivery.setLastResponseCode(null);
            log.warn("Webhook delivery {} attempt {} failed: {}", deliveryId, attempt, e.getMessage());
            handleFailure(delivery, attempt);
        }
        deliveryRepository.save(delivery);
    }

    private void handleFailure(WebhookDelivery delivery, int attempt) {
        metrics.recordWebhookAttempt("failed");
        int maxAttempts = gatewayProperties.webhook().maxAttempts();
        if (attempt >= maxAttempts) {
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setNextRetryAt(null);
            retryQueue.remove(delivery.getId());
            log.warn("Webhook delivery {} permanently failed after {} attempts", delivery.getId(), attempt);
        } else {
            Duration delay = BACKOFF[Math.min(attempt - 1, BACKOFF.length - 1)];
            Instant next = Instant.now().plus(delay);
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextRetryAt(next);
            retryQueue.schedule(delivery.getId(), next);
            log.info("Webhook delivery {} will retry at {} (attempt {} of {})",
                    delivery.getId(), next, attempt, maxAttempts);
        }
    }
}
