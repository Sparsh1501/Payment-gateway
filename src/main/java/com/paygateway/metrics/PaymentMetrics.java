package com.paygateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Centralised Micrometer instrumentation for the gateway.
 */
@Component
public class PaymentMetrics {

    private final MeterRegistry registry;

    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** payments.total — counter tagged by provider, status, currency. */
    public void recordPayment(String provider, String status, String currency, double amount) {
        Counter.builder("payments.total")
                .tag("provider", provider)
                .tag("status", status)
                .tag("currency", currency)
                .register(registry)
                .increment();

        // payments.amount.sum — total volume processed
        registry.counter("payments.amount.sum", "provider", provider, "currency", currency)
                .increment(amount);
    }

    /** provider.latency — timer per provider. */
    public void recordProviderLatency(String provider, String operation, long millis) {
        Timer.builder("provider.latency")
                .tag("provider", provider)
                .tag("operation", operation)
                .register(registry)
                .record(millis, TimeUnit.MILLISECONDS);
    }

    /** webhooks.delivery.attempts — counter tagged by status. */
    public void recordWebhookAttempt(String status) {
        registry.counter("webhooks.delivery.attempts", "status", status).increment();
    }

    public void checkoutSessionCreated() {
        registry.counter("checkout.sessions.created").increment();
    }

    public void checkoutSessionCompleted() {
        registry.counter("checkout.sessions.completed").increment();
    }
}
