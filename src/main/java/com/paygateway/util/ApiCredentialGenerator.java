package com.paygateway.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Generates UUID-based API key / secret pairs for merchants.
 */
@Component
public class ApiCredentialGenerator {

    public String generateApiKey() {
        return "pk_" + UUID.randomUUID().toString().replace("-", "");
    }

    public String generateApiSecret() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
