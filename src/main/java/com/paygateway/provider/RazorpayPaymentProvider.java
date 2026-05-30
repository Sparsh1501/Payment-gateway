package com.paygateway.provider;

import com.paygateway.config.props.RazorpayProperties;
import com.paygateway.entity.enums.PaymentProviderType;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Razorpay adapter. Uses the Razorpay Java SDK when live keys are configured,
 * otherwise runs in simulation mode.
 */
@Slf4j
@Component
public class RazorpayPaymentProvider implements PaymentProvider {

    private final RazorpayProperties properties;
    private final boolean simulation;

    public RazorpayPaymentProvider(RazorpayProperties properties) {
        this.properties = properties;
        this.simulation = properties.keyId() == null || properties.keyId().contains("dummy");
        if (simulation) {
            log.warn("RazorpayPaymentProvider running in SIMULATION mode (no live key configured)");
        }
    }

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.RAZORPAY;
    }

    @Override
    public PaymentResult createCharge(PaymentRequest request) {
        if (simulation) {
            return PaymentResult.success("pay_sim_" + UUID.randomUUID().toString().replace("-", ""), "captured");
        }
        try {
            RazorpayClient client = new RazorpayClient(properties.keyId(), properties.keySecret());
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.amount().movePointRight(2).longValueExact());
            orderRequest.put("currency", request.currency().toUpperCase());
            orderRequest.put("payment_capture", true);
            if (request.idempotencyKey() != null) {
                orderRequest.put("receipt", request.idempotencyKey());
            }
            com.razorpay.Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");
            log.info("Razorpay order created id={}", orderId);
            return PaymentResult.success(orderId, "created");
        } catch (RazorpayException e) {
            log.error("Razorpay createCharge failed: {}", e.getMessage(), e);
            return PaymentResult.failure(e.getMessage());
        }
    }

    @Override
    public PaymentResult getCharge(String providerPaymentId) {
        if (simulation) {
            return PaymentResult.success(providerPaymentId, "captured");
        }
        try {
            RazorpayClient client = new RazorpayClient(properties.keyId(), properties.keySecret());
            com.razorpay.Payment payment = client.payments.fetch(providerPaymentId);
            return PaymentResult.success(providerPaymentId, payment.get("status"));
        } catch (RazorpayException e) {
            log.error("Razorpay getCharge failed: {}", e.getMessage(), e);
            return PaymentResult.failure(e.getMessage());
        }
    }

    @Override
    public RefundResult createRefund(RefundRequest request) {
        if (simulation) {
            return RefundResult.success("rfnd_sim_" + UUID.randomUUID().toString().replace("-", ""), "processed");
        }
        try {
            RazorpayClient client = new RazorpayClient(properties.keyId(), properties.keySecret());
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", request.amount().movePointRight(2).longValueExact());
            com.razorpay.Refund refund = client.payments.refund(request.providerPaymentId(), refundRequest);
            String refundId = refund.get("id");
            log.info("Razorpay refund created id={}", refundId);
            return RefundResult.success(refundId, "processed");
        } catch (RazorpayException e) {
            log.error("Razorpay createRefund failed: {}", e.getMessage(), e);
            return RefundResult.failure(e.getMessage());
        }
    }
}
