package com.paygateway.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stripe provider configuration bound from the {@code stripe.*} namespace.
 */
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        String baseUrl
) {
}
