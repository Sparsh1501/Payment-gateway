package com.paygateway.repository;

import com.paygateway.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    List<WebhookEndpoint> findByMerchantId(UUID merchantId);

    List<WebhookEndpoint> findByMerchantIdAndActiveTrue(UUID merchantId);

    Optional<WebhookEndpoint> findByIdAndMerchantId(UUID id, UUID merchantId);
}
