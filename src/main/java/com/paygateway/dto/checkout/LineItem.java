package com.paygateway.dto.checkout;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineItem(
        @NotNull String name,
        @NotNull BigDecimal amount,
        @NotNull @Min(1) Integer quantity
) {
    public BigDecimal lineTotal() {
        return amount.multiply(BigDecimal.valueOf(quantity));
    }
}
