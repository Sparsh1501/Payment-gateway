package com.paygateway.auth;

import java.util.UUID;

/**
 * Authenticated merchant identity stored as the Spring Security principal.
 */
public record MerchantPrincipal(UUID merchantId, String email) {
}
