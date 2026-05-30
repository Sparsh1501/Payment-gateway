package com.paygateway.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration bound from the {@code jwt.*} namespace.
 *
 * @param secret              HMAC signing secret (>= 64 bytes recommended)
 * @param accessTokenExpiry   access token TTL in milliseconds
 * @param refreshTokenExpiry  refresh token TTL in milliseconds
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiry,
        long refreshTokenExpiry
) {
}
