package com.paygateway.provider;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        UUID refundId,
        String providerPaymentId,
        BigDecimal amount,
        String currency,
        String reason
) {
}
