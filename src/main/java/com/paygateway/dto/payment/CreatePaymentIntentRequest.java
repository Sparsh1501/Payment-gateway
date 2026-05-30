package com.paygateway.dto.payment;

import com.paygateway.entity.enums.PaymentProviderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.Map;

public record CreatePaymentIntentRequest(
        @NotNull @DecimalMin(value = "0.50", message = "amount must be at least 0.50") BigDecimal amount,
        @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        @NotNull PaymentProviderType provider,
        Map<String, Object> metadata,
        String idempotencyKey
) {
}
