package com.paygateway.dto.payment;

import com.paygateway.entity.PaymentIntent;
import com.paygateway.util.JsonUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentIntentResponse(
        UUID id,
        UUID merchantId,
        BigDecimal amount,
        String currency,
        String status,
        String provider,
        String providerPaymentId,
        String idempotencyKey,
        Map<String, Object> metadata,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    @SuppressWarnings("unchecked")
    public static PaymentIntentResponse from(PaymentIntent p, JsonUtil jsonUtil) {
        Map<String, Object> meta = p.getMetadata() == null
                ? null : jsonUtil.fromJson(p.getMetadata(), Map.class);
        return new PaymentIntentResponse(
                p.getId(), p.getMerchantId(), p.getAmount(), p.getCurrency(),
                p.getStatus().name(), p.getProvider().name(), p.getProviderPaymentId(),
                p.getIdempotencyKey(), meta, p.getFailureReason(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
