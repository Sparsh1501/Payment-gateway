package com.paygateway.dto.checkout;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutSessionResponse(
        UUID id,
        UUID paymentIntentId,
        String checkoutUrl,
        String status,
        BigDecimal amount,
        String currency,
        Instant expiresAt
) {
}
