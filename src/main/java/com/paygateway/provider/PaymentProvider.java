package com.paygateway.provider;

import com.paygateway.entity.enums.PaymentProviderType;

/**
 * Adapter contract implemented by every payment provider (Stripe, Razorpay, ...).
 * Implementations are synchronous; the orchestration layer invokes them
 * asynchronously and wraps each call in a circuit breaker.
 */
public interface PaymentProvider {

    PaymentProviderType type();

    PaymentResult createCharge(PaymentRequest request);

    PaymentResult getCharge(String providerPaymentId);

    RefundResult createRefund(RefundRequest request);
}
