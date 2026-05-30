package com.paygateway.dto.auth;

public record ApiKeyResponse(
        String apiKey,
        String apiSecret
) {
}
