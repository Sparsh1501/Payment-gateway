package com.paygateway.service;

import com.paygateway.dto.payment.CreatePaymentIntentRequest;
import com.paygateway.dto.payment.PaymentIntentResponse;
import com.paygateway.entity.PaymentIntent;
import com.paygateway.entity.Transaction;
import com.paygateway.entity.enums.PaymentProviderType;
import com.paygateway.entity.enums.PaymentStatus;
import com.paygateway.entity.enums.TransactionType;
import com.paygateway.exception.BusinessException;
import com.paygateway.exception.ResourceNotFoundException;
import com.paygateway.metrics.PaymentMetrics;
import com.paygateway.provider.PaymentRequest;
import com.paygateway.provider.ProviderGateway;
import com.paygateway.repository.PaymentIntentRepository;
import com.paygateway.repository.TransactionRepository;
import com.paygateway.util.JsonUtil;
import com.paygateway.webhook.WebhookEvents;
import com.paygateway.webhook.WebhookPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final TransactionRepository transactionRepository;
    private final ProviderGateway providerGateway;
    private final FeeCalculator feeCalculator;
    private final WebhookPublisher webhookPublisher;
    private final PaymentMetrics metrics;
    private final IdempotencyService idempotencyService;
    private final JsonUtil jsonUtil;

    @Transactional
    public PaymentIntentResponse create(UUID merchantId, CreatePaymentIntentRequest request, String idempotencyKey) {
        String effectiveKey = idempotencyKey != null ? idempotencyKey : request.idempotencyKey();

        if (effectiveKey != null) {
            // 1) fast path: Redis cache (fails open if Redis is down)
            var cached = idempotencyService.lookup(merchantId, effectiveKey);
            if (cached.isPresent()) {
                log.info("Idempotency cache hit for key {}", effectiveKey);
                return jsonUtil.fromJson(cached.get(), PaymentIntentResponse.class);
            }
            // 2) durable backstop: unique (merchant, key) row already persisted
            var existing = paymentIntentRepository.findByMerchantIdAndIdempotencyKey(merchantId, effectiveKey);
            if (existing.isPresent()) {
                log.info("Returning existing payment-intent for idempotency key {}", effectiveKey);
                PaymentIntentResponse response = toResponse(existing.get());
                idempotencyService.store(merchantId, effectiveKey, jsonUtil.toJson(response));
                return response;
            }
        }

        PaymentIntent intent = PaymentIntent.builder()
                .merchantId(merchantId)
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .status(PaymentStatus.CREATED)
                .provider(request.provider())
                .idempotencyKey(effectiveKey)
                .metadata(jsonUtil.toJson(request.metadata()))
                .build();
        intent = paymentIntentRepository.save(intent);
        log.info("Created payment-intent {} for merchant {} amount {} {}",
                intent.getId(), merchantId, intent.getAmount(), intent.getCurrency());

        metrics.recordPayment(intent.getProvider().name(), intent.getStatus().name(),
                intent.getCurrency(), 0);
        webhookPublisher.publish(merchantId, WebhookEvents.PAYMENT_CREATED, toResponse(intent));

        PaymentIntentResponse response = toResponse(intent);
        if (effectiveKey != null) {
            idempotencyService.store(merchantId, effectiveKey, jsonUtil.toJson(response));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PaymentIntentResponse get(UUID merchantId, UUID id) {
        return toResponse(loadOwned(merchantId, id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentIntentResponse> list(UUID merchantId, Pageable pageable) {
        return paymentIntentRepository.findByMerchantId(merchantId, pageable).map(this::toResponse);
    }

    /**
     * Confirms a payment intent: invokes the provider (async + circuit breaker),
     * transitions state atomically, records a transaction and fires webhooks.
     */
    @Transactional
    public PaymentIntentResponse confirm(UUID merchantId, UUID id) {
        PaymentIntent intent = loadOwned(merchantId, id);
        if (intent.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(intent);
        }
        if (intent.getStatus() != PaymentStatus.CREATED && intent.getStatus() != PaymentStatus.PROCESSING) {
            throw new BusinessException("INVALID_STATE",
                    "Cannot confirm a payment-intent in state " + intent.getStatus());
        }

        intent.setStatus(PaymentStatus.PROCESSING);
        paymentIntentRepository.saveAndFlush(intent);
        webhookPublisher.publish(merchantId, WebhookEvents.PAYMENT_PROCESSING, toResponse(intent));

        PaymentRequest providerRequest = new PaymentRequest(
                intent.getId(), intent.getAmount(), intent.getCurrency(),
                "Payment for intent " + intent.getId(),
                intent.getIdempotencyKey() != null ? intent.getIdempotencyKey() : intent.getId().toString());

        try {
            ProviderGateway.ChargeOutcome outcome = providerGateway.charge(intent.getProvider(), providerRequest).join();
            applySuccess(intent, outcome.provider(), outcome.result().providerPaymentId());
            log.info("Payment-intent {} succeeded via {}", intent.getId(), outcome.provider());
        } catch (Exception e) {
            applyFailure(intent, rootMessage(e));
            log.warn("Payment-intent {} failed: {}", intent.getId(), rootMessage(e));
        }
        return toResponse(intent);
    }

    /**
     * Applies a status update received from an inbound provider webhook
     * (Stripe / Razorpay), keyed by the provider payment id.
     */
    @Transactional
    public void handleProviderUpdate(String providerPaymentId, boolean success, String rawStatus) {
        var opt = paymentIntentRepository.findByProviderPaymentId(providerPaymentId);
        if (opt.isEmpty()) {
            log.warn("Inbound provider webhook references unknown provider payment id {}", providerPaymentId);
            return;
        }
        PaymentIntent intent = opt.get();
        if (success && intent.getStatus() != PaymentStatus.SUCCESS) {
            applySuccess(intent, intent.getProvider(), providerPaymentId);
            log.info("Payment-intent {} marked SUCCESS via inbound webhook", intent.getId());
        } else if (!success && intent.getStatus() != PaymentStatus.SUCCESS
                && intent.getStatus() != PaymentStatus.FAILED) {
            applyFailure(intent, "Provider reported: " + rawStatus);
            log.info("Payment-intent {} marked FAILED via inbound webhook", intent.getId());
        }
    }

    @Transactional
    public PaymentIntentResponse cancel(UUID merchantId, UUID id) {
        PaymentIntent intent = loadOwned(merchantId, id);
        if (intent.getStatus() == PaymentStatus.SUCCESS) {
            throw new BusinessException("INVALID_STATE", "Cannot cancel a successful payment");
        }
        if (intent.getStatus() == PaymentStatus.CANCELLED) {
            return toResponse(intent);
        }
        intent.setStatus(PaymentStatus.CANCELLED);
        paymentIntentRepository.save(intent);
        metrics.recordPayment(intent.getProvider().name(), PaymentStatus.CANCELLED.name(),
                intent.getCurrency(), 0);
        webhookPublisher.publish(merchantId, WebhookEvents.PAYMENT_CANCELLED, toResponse(intent));
        return toResponse(intent);
    }

    private void applySuccess(PaymentIntent intent, PaymentProviderType usedProvider, String providerPaymentId) {
        intent.setStatus(PaymentStatus.SUCCESS);
        intent.setProvider(usedProvider);
        intent.setProviderPaymentId(providerPaymentId);
        intent.setFailureReason(null);
        paymentIntentRepository.save(intent);

        var fee = feeCalculator.fee(intent.getAmount());
        Transaction txn = Transaction.builder()
                .paymentIntentId(intent.getId())
                .type(TransactionType.PAYMENT)
                .amount(intent.getAmount())
                .currency(intent.getCurrency())
                .fee(fee)
                .net(intent.getAmount().subtract(fee))
                .provider(usedProvider)
                .build();
        transactionRepository.save(txn);

        metrics.recordPayment(usedProvider.name(), PaymentStatus.SUCCESS.name(),
                intent.getCurrency(), intent.getAmount().doubleValue());
        webhookPublisher.publish(intent.getMerchantId(), WebhookEvents.PAYMENT_SUCCESS, toResponse(intent));
    }

    private void applyFailure(PaymentIntent intent, String reason) {
        intent.setStatus(PaymentStatus.FAILED);
        intent.setFailureReason(reason);
        paymentIntentRepository.save(intent);
        metrics.recordPayment(intent.getProvider().name(), PaymentStatus.FAILED.name(),
                intent.getCurrency(), 0);
        webhookPublisher.publish(intent.getMerchantId(), WebhookEvents.PAYMENT_FAILED, toResponse(intent));
    }

    private PaymentIntent loadOwned(UUID merchantId, UUID id) {
        return paymentIntentRepository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment intent not found: " + id));
    }

    private PaymentIntentResponse toResponse(PaymentIntent intent) {
        return PaymentIntentResponse.from(intent, jsonUtil);
    }

    private String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
