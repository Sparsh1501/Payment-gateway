package com.paygateway.dto.checkout;

import com.paygateway.entity.enums.PaymentProviderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateCheckoutSessionRequest(
        @NotNull @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code") String currency,
        @NotNull PaymentProviderType provider,
        @NotEmpty @Valid List<LineItem> lineItems,
        @NotNull @Pattern(regexp = "^https?://.+") String successUrl,
        @NotNull @Pattern(regexp = "^https?://.+") String cancelUrl
) {
}
