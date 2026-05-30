package com.paygateway.dto.webhook;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateWebhookEndpointRequest(
        @NotNull @Pattern(regexp = "^https?://.+", message = "url must be a valid http(s) URL") String url,
        @NotEmpty List<String> events
) {
}
