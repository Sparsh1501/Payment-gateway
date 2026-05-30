package com.paygateway.webhook;

import com.paygateway.config.props.RazorpayProperties;
import com.paygateway.config.props.StripeProperties;
import com.paygateway.exception.BusinessException;
import com.paygateway.service.PaymentService;
import com.paygateway.util.JsonUtil;
import com.razorpay.Utils;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Verifies and processes inbound webhooks from Stripe and Razorpay, mapping
 * provider event types onto internal payment-intent state transitions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundWebhookService {

    private final StripeProperties stripeProperties;
    private final RazorpayProperties razorpayProperties;
    private final PaymentService paymentService;
    private final JsonUtil jsonUtil;

    public void handleStripe(String payload, String signature) {
        if (!isDummy(stripeProperties.webhookSecret())) {
            try {
                Webhook.constructEvent(payload, signature, stripeProperties.webhookSecret());
            } catch (Exception e) {
                log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
                throw new BusinessException("INVALID_SIGNATURE", "Stripe signature verification failed");
            }
        } else {
            log.warn("Stripe webhook secret is a placeholder - skipping signature verification (sandbox)");
        }

        Map<String, Object> event = jsonUtil.fromJson(payload, Map.class);
        String type = str(event.get("type"));
        String objectId = nestedObjectId(event);
        if (objectId == null) {
            log.warn("Stripe webhook {} had no resolvable object id", type);
            return;
        }
        boolean success = type != null && (type.contains("succeeded"));
        boolean failure = type != null && (type.contains("failed") || type.contains("canceled"));
        if (success) {
            paymentService.handleProviderUpdate(objectId, true, type);
        } else if (failure) {
            paymentService.handleProviderUpdate(objectId, false, type);
        } else {
            log.info("Stripe webhook {} ignored (no state transition)", type);
        }
    }

    public void handleRazorpay(String payload, String signature) {
        if (!isDummy(razorpayProperties.webhookSecret())) {
            try {
                boolean valid = Utils.verifyWebhookSignature(payload, signature, razorpayProperties.webhookSecret());
                if (!valid) {
                    throw new BusinessException("INVALID_SIGNATURE", "Razorpay signature verification failed");
                }
            } catch (BusinessException be) {
                throw be;
            } catch (Exception e) {
                log.warn("Razorpay webhook signature verification error: {}", e.getMessage());
                throw new BusinessException("INVALID_SIGNATURE", "Razorpay signature verification failed");
            }
        } else {
            log.warn("Razorpay webhook secret is a placeholder - skipping signature verification (sandbox)");
        }

        Map<String, Object> event = jsonUtil.fromJson(payload, Map.class);
        String type = str(event.get("event"));
        String paymentId = razorpayPaymentId(event);
        if (paymentId == null) {
            log.warn("Razorpay webhook {} had no resolvable payment id", type);
            return;
        }
        if ("payment.captured".equals(type) || "order.paid".equals(type)) {
            paymentService.handleProviderUpdate(paymentId, true, type);
        } else if ("payment.failed".equals(type)) {
            paymentService.handleProviderUpdate(paymentId, false, type);
        } else {
            log.info("Razorpay webhook {} ignored (no state transition)", type);
        }
    }

    @SuppressWarnings("unchecked")
    private String nestedObjectId(Map<String, Object> event) {
        Object data = event.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object object = ((Map<String, Object>) dataMap).get("object");
            if (object instanceof Map<?, ?> objMap) {
                return str(((Map<String, Object>) objMap).get("id"));
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String razorpayPaymentId(Map<String, Object> event) {
        Object payloadObj = event.get("payload");
        if (payloadObj instanceof Map<?, ?> payloadMap) {
            Object payment = ((Map<String, Object>) payloadMap).get("payment");
            if (payment instanceof Map<?, ?> paymentMap) {
                Object entity = ((Map<String, Object>) paymentMap).get("entity");
                if (entity instanceof Map<?, ?> entityMap) {
                    return str(((Map<String, Object>) entityMap).get("id"));
                }
            }
        }
        return null;
    }

    private boolean isDummy(String secret) {
        return secret == null || secret.isBlank() || secret.contains("dummy");
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
