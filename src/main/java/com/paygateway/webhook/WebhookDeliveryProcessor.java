package com.paygateway.webhook;

import com.paygateway.entity.enums.DeliveryStatus;
import com.paygateway.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Drives webhook delivery: every 30 seconds it drains due items from the Redis
 * retry queue, with a DB sweep as a safety net should Redis lose entries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDeliveryProcessor {

    private final WebhookRetryQueue retryQueue;
    private final WebhookService webhookService;
    private final WebhookDeliveryRepository deliveryRepository;

    @Scheduled(fixedDelayString = "${gateway.webhook.poll-interval-ms:30000}")
    public void processDueDeliveries() {
        Set<UUID> due = new LinkedHashSet<>(retryQueue.pollDue(200));

        // DB safety net: pick up anything pending whose retry time has passed.
        List<UUID> fromDb = deliveryRepository
                .findTop100ByStatusAndNextRetryAtLessThanEqual(DeliveryStatus.PENDING, Instant.now())
                .stream()
                .map(d -> d.getId())
                .toList();
        due.addAll(fromDb);

        if (due.isEmpty()) {
            return;
        }
        log.debug("Processing {} due webhook deliveries", due.size());
        for (UUID deliveryId : due) {
            try {
                webhookService.attemptDelivery(deliveryId);
            } catch (Exception e) {
                log.error("Error processing webhook delivery {}: {}", deliveryId, e.getMessage(), e);
            }
        }
    }
}
