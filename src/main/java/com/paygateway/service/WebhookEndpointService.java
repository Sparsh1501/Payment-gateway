package com.paygateway.service;

import com.paygateway.dto.webhook.CreateWebhookEndpointRequest;
import com.paygateway.dto.webhook.WebhookDeliveryResponse;
import com.paygateway.dto.webhook.WebhookEndpointResponse;
import com.paygateway.entity.WebhookEndpoint;
import com.paygateway.exception.ResourceNotFoundException;
import com.paygateway.repository.WebhookDeliveryRepository;
import com.paygateway.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEndpointService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    @Transactional
    public WebhookEndpointResponse create(UUID merchantId, CreateWebhookEndpointRequest request) {
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .merchantId(merchantId)
                .url(request.url())
                .secret("whsec_" + UUID.randomUUID().toString().replace("-", ""))
                .events(request.events().toArray(new String[0]))
                .active(true)
                .build();
        endpoint = endpointRepository.save(endpoint);
        log.info("Registered webhook endpoint {} for merchant {}", endpoint.getId(), merchantId);
        return WebhookEndpointResponse.from(endpoint);
    }

    @Transactional(readOnly = true)
    public List<WebhookEndpointResponse> list(UUID merchantId) {
        return endpointRepository.findByMerchantId(merchantId).stream()
                .map(WebhookEndpointResponse::from)
                .toList();
    }

    @Transactional
    public void delete(UUID merchantId, UUID id) {
        WebhookEndpoint endpoint = endpointRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint not found: " + id));
        endpointRepository.delete(endpoint);
        log.info("Deleted webhook endpoint {} for merchant {}", id, merchantId);
    }

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryResponse> listDeliveries(UUID merchantId, Pageable pageable) {
        List<UUID> endpointIds = endpointRepository.findByMerchantId(merchantId).stream()
                .map(WebhookEndpoint::getId)
                .toList();
        if (endpointIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return deliveryRepository.findByWebhookEndpointIdIn(endpointIds, pageable)
                .map(WebhookDeliveryResponse::from);
    }
}
