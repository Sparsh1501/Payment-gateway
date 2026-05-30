package com.paygateway.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Razorpay provider configuration bound from the {@code razorpay.*} namespace.
 */
@ConfigurationProperties(prefix = "razorpay")
public record RazorpayProperties(
        String keyId,
        String keySecret,
        String webhookSecret
) {
}
