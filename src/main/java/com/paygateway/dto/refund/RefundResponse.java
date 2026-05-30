package com.paygateway.dto.refund;

import com.paygateway.entity.Refund;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentIntentId,
        UUID merchantId,
        BigDecimal amount,
        String reason,
        String status,
        String providerRefundId,
        Instant createdAt
) {
    public static RefundResponse from(Refund r) {
        return new RefundResponse(
                r.getId(), r.getPaymentIntentId(), r.getMerchantId(), r.getAmount(),
                r.getReason(), r.getStatus().name(), r.getProviderRefundId(), r.getCreatedAt());
    }
}
