package com.paygateway.provider;

public record RefundResult(
        boolean success,
        String providerRefundId,
        String rawStatus,
        String errorMessage
) {

    public static RefundResult success(String providerRefundId, String rawStatus) {
        return new RefundResult(true, providerRefundId, rawStatus, null);
    }

    public static RefundResult failure(String errorMessage) {
        return new RefundResult(false, null, "failed", errorMessage);
    }
}
