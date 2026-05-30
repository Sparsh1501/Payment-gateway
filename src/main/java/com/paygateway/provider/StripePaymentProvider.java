package com.paygateway.provider;

import com.paygateway.config.props.StripeProperties;
import com.paygateway.entity.enums.PaymentProviderType;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stripe adapter. Uses the Stripe Java SDK when a real secret key is configured,
 * otherwise runs in simulation mode so the gateway is fully exercisable locally.
 */
@Slf4j
@Component
public class StripePaymentProvider implements PaymentProvider {

    private final StripeProperties properties;
    private final boolean simulation;

    public StripePaymentProvider(StripeProperties properties) {
        this.properties = properties;
        this.simulation = properties.secretKey() == null || properties.secretKey().contains("dummy");
        if (simulation) {
            log.warn("StripePaymentProvider running in SIMULATION mode (no live secret key configured)");
        }
    }

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public PaymentResult createCharge(PaymentRequest request) {
        if (simulation) {
            return PaymentResult.success("pi_sim_" + UUID.randomUUID().toString().replace("-", ""), "succeeded");
        }
        try {
            Stripe.apiKey = properties.secretKey();
            Map<String, Object> params = new HashMap<>();
            params.put("amount", request.amount().movePointRight(2).longValueExact());
            params.put("currency", request.currency().toLowerCase());
            params.put("confirm", true);
            params.put("payment_method", "pm_card_visa");
            params.put("automatic_payment_methods",
                    Map.of("enabled", true, "allow_redirects", "never"));
            if (request.description() != null) {
                params.put("description", request.description());
            }
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();
            PaymentIntent intent = PaymentIntent.create(params, options);
            log.info("Stripe charge created id={} status={}", intent.getId(), intent.getStatus());
            boolean ok = "succeeded".equals(intent.getStatus()) || "requires_capture".equals(intent.getStatus());
            return ok
                    ? PaymentResult.success(intent.getId(), intent.getStatus())
                    : new PaymentResult(false, intent.getId(), intent.getStatus(), "Stripe status: " + intent.getStatus());
        } catch (StripeException e) {
            log.error("Stripe createCharge failed: {}", e.getMessage(), e);
            return PaymentResult.failure(e.getMessage());
        }
    }

    @Override
    public PaymentResult getCharge(String providerPaymentId) {
        if (simulation) {
            return PaymentResult.success(providerPaymentId, "succeeded");
        }
        try {
            Stripe.apiKey = properties.secretKey();
            PaymentIntent intent = PaymentIntent.retrieve(providerPaymentId);
            return PaymentResult.success(intent.getId(), intent.getStatus());
        } catch (StripeException e) {
            log.error("Stripe getCharge failed: {}", e.getMessage(), e);
            return PaymentResult.failure(e.getMessage());
        }
    }

    @Override
    public RefundResult createRefund(RefundRequest request) {
        if (simulation) {
            return RefundResult.success("re_sim_" + UUID.randomUUID().toString().replace("-", ""), "succeeded");
        }
        try {
            Stripe.apiKey = properties.secretKey();
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", request.providerPaymentId());
            params.put("amount", request.amount().movePointRight(2).longValueExact());
            if (request.reason() != null) {
                params.put("metadata", Map.of("reason", request.reason()));
            }
            Refund refund = Refund.create(params);
            log.info("Stripe refund created id={} status={}", refund.getId(), refund.getStatus());
            return RefundResult.success(refund.getId(), refund.getStatus());
        } catch (StripeException e) {
            log.error("Stripe createRefund failed: {}", e.getMessage(), e);
            return RefundResult.failure(e.getMessage());
        }
    }
}
