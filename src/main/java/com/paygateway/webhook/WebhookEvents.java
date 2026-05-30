package com.paygateway.webhook;

/**
 * Canonical outbound webhook event types.
 */
public final class WebhookEvents {

    public static final String PAYMENT_CREATED = "payment.created";
    public static final String PAYMENT_PROCESSING = "payment.processing";
    public static final String PAYMENT_SUCCESS = "payment.success";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_CANCELLED = "payment.cancelled";
    public static final String REFUND_CREATED = "refund.created";
    public static final String REFUND_SUCCESS = "refund.success";
    public static final String REFUND_FAILED = "refund.failed";

    private WebhookEvents() {
    }
}
