package com.paygateway.provider;

import com.paygateway.entity.enums.PaymentProviderType;
import com.paygateway.exception.ProviderException;
import com.paygateway.metrics.PaymentMetrics;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Routes provider calls to the correct adapter, executes them asynchronously,
 * and wraps every call in a Resilience4j circuit breaker.
 *
 * <p>Fallback rule: if the Stripe circuit breaker is OPEN (i.e. Stripe has been
 * failing), new STRIPE charges are automatically re-routed to Razorpay.</p>
 */
@Slf4j
@Service
public class ProviderGateway {

    private final Map<PaymentProviderType, PaymentProvider> providers = new EnumMap<>(PaymentProviderType.class);
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Executor executor;
    private final PaymentMetrics metrics;

    public ProviderGateway(List<PaymentProvider> providerList,
                           CircuitBreakerRegistry circuitBreakerRegistry,
                           @org.springframework.beans.factory.annotation.Qualifier("providerExecutor") Executor executor,
                           PaymentMetrics metrics) {
        providerList.forEach(p -> providers.put(p.type(), p));
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.executor = executor;
        this.metrics = metrics;
    }

    public CompletableFuture<ChargeOutcome> charge(PaymentProviderType requested, PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> chargeWithFallback(requested, request), executor);
    }

    public CompletableFuture<RefundResult> refund(PaymentProviderType provider, RefundRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            CircuitBreaker cb = breaker(provider);
            try {
                return cb.executeSupplier(() -> {
                    RefundResult result = providers.get(provider).createRefund(request);
                    if (!result.success()) {
                        throw new ProviderException("Refund failed: " + result.errorMessage());
                    }
                    return result;
                });
            } catch (CallNotPermittedException e) {
                log.warn("{} circuit open; refund rejected", provider);
                return RefundResult.failure(provider + " temporarily unavailable");
            } catch (ProviderException e) {
                return RefundResult.failure(e.getMessage());
            }
        }, executor);
    }

    private ChargeOutcome chargeWithFallback(PaymentProviderType requested, PaymentRequest request) {
        PaymentProviderType effective = requested;
        if (requested == PaymentProviderType.STRIPE && isOpen(PaymentProviderType.STRIPE)) {
            log.warn("Stripe circuit OPEN - routing payment-intent {} to Razorpay fallback",
                    request.paymentIntentId());
            effective = PaymentProviderType.RAZORPAY;
        }

        try {
            PaymentResult result = invokeCharge(effective, request);
            return new ChargeOutcome(effective, result);
        } catch (CallNotPermittedException e) {
            log.warn("{} circuit open mid-flight for intent {}", effective, request.paymentIntentId());
            if (effective == PaymentProviderType.STRIPE) {
                return new ChargeOutcome(PaymentProviderType.RAZORPAY,
                        invokeChargeSafe(PaymentProviderType.RAZORPAY, request));
            }
            throw new ProviderException(effective + " is temporarily unavailable", e);
        } catch (ProviderException e) {
            if (effective == PaymentProviderType.STRIPE) {
                log.warn("Stripe charge failed for intent {}, attempting Razorpay fallback: {}",
                        request.paymentIntentId(), e.getMessage());
                return new ChargeOutcome(PaymentProviderType.RAZORPAY,
                        invokeChargeSafe(PaymentProviderType.RAZORPAY, request));
            }
            throw e;
        }
    }

    private PaymentResult invokeCharge(PaymentProviderType type, PaymentRequest request) {
        CircuitBreaker cb = breaker(type);
        Supplier<PaymentResult> supplier = () -> {
            long start = System.currentTimeMillis();
            try {
                PaymentResult result = providers.get(type).createCharge(request);
                if (!result.success()) {
                    throw new ProviderException("Charge declined: " + result.errorMessage());
                }
                return result;
            } finally {
                metrics.recordProviderLatency(type.name(), "charge", System.currentTimeMillis() - start);
            }
        };
        return cb.executeSupplier(supplier);
    }

    private PaymentResult invokeChargeSafe(PaymentProviderType type, PaymentRequest request) {
        try {
            return invokeCharge(type, request);
        } catch (CallNotPermittedException e) {
            throw new ProviderException(type + " is temporarily unavailable", e);
        }
    }

    private boolean isOpen(PaymentProviderType type) {
        return breaker(type).getState() == CircuitBreaker.State.OPEN;
    }

    private CircuitBreaker breaker(PaymentProviderType type) {
        return circuitBreakerRegistry.circuitBreaker(type.name().toLowerCase());
    }

    public PaymentResult getCharge(PaymentProviderType type, String providerPaymentId) {
        return providers.get(type).getCharge(providerPaymentId);
    }

    public record ChargeOutcome(PaymentProviderType provider, PaymentResult result) {
    }
}
