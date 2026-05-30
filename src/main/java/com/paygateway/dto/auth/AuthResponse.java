package com.paygateway.dto.auth;

import java.util.UUID;

public record AuthResponse(
        UUID merchantId,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
