package com.paygateway.repository;

import com.paygateway.entity.WebhookDelivery;
import com.paygateway.entity.enums.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    Page<WebhookDelivery> findByWebhookEndpointIdIn(List<UUID> endpointIds, Pageable pageable);

    List<WebhookDelivery> findTop100ByStatusAndNextRetryAtLessThanEqual(DeliveryStatus status, Instant cutoff);
}
