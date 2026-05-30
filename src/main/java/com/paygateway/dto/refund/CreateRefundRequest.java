package com.paygateway.dto.refund;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRefundRequest(
        @NotNull UUID paymentIntentId,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be positive") BigDecimal amount,
        String reason
) {
}
