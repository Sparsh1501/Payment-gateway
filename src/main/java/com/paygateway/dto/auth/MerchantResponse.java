package com.paygateway.dto.auth;

import com.paygateway.entity.Merchant;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID id,
        String businessName,
        String email,
        String apiKey,
        String status,
        Instant createdAt
) {
    public static MerchantResponse from(Merchant m) {
        return new MerchantResponse(
                m.getId(), m.getBusinessName(), m.getEmail(),
                m.getApiKey(), m.getStatus().name(), m.getCreatedAt());
    }
}
