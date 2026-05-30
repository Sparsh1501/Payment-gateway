package com.paygateway.dto.auth;

import com.paygateway.entity.Merchant;

import java.util.UUID;

/**
 * Returned once at registration time; includes the API secret which is never
 * exposed again in full elsewhere.
 */
public record RegisterResponse(
        UUID merchantId,
        String businessName,
        String email,
        String apiKey,
        String apiSecret
) {
    public static RegisterResponse from(Merchant m) {
        return new RegisterResponse(
                m.getId(), m.getBusinessName(), m.getEmail(),
                m.getApiKey(), m.getApiSecret());
    }
}
