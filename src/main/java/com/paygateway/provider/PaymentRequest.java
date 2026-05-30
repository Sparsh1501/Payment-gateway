package com.paygateway.provider;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Provider-agnostic charge request.
 */
public record PaymentRequest(
        UUID paymentIntentId,
        BigDecimal amount,
        String currency,
        String description,
        String idempotencyKey
) {
}
