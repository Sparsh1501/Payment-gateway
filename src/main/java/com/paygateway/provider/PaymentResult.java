package com.paygateway.provider;

/**
 * Provider-agnostic charge result.
 */
public record PaymentResult(
        boolean success,
        String providerPaymentId,
        String rawStatus,
        String errorMessage
) {

    public static PaymentResult success(String providerPaymentId, String rawStatus) {
        return new PaymentResult(true, providerPaymentId, rawStatus, null);
    }

    public static PaymentResult failure(String errorMessage) {
        return new PaymentResult(false, null, "failed", errorMessage);
    }
}
