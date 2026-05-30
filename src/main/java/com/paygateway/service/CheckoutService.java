package com.paygateway.service;

import com.paygateway.config.props.GatewayProperties;
import com.paygateway.dto.checkout.CheckoutSessionResponse;
import com.paygateway.dto.checkout.CreateCheckoutSessionRequest;
import com.paygateway.dto.checkout.LineItem;
import com.paygateway.dto.payment.CreatePaymentIntentRequest;
import com.paygateway.dto.payment.PaymentIntentResponse;
import com.paygateway.entity.CheckoutSession;
import com.paygateway.entity.enums.CheckoutStatus;
import com.paygateway.exception.BusinessException;
import com.paygateway.exception.ResourceNotFoundException;
import com.paygateway.metrics.PaymentMetrics;
import com.paygateway.repository.CheckoutSessionRepository;
import com.paygateway.repository.PaymentIntentRepository;
import com.paygateway.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentService paymentService;
    private final GatewayProperties gatewayProperties;
    private final PaymentMetrics metrics;
    private final JsonUtil jsonUtil;

    @Transactional
    public CheckoutSessionResponse create(UUID merchantId, CreateCheckoutSessionRequest request) {
        BigDecimal total = request.lineItems().stream()
                .map(LineItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CreatePaymentIntentRequest intentRequest = new CreatePaymentIntentRequest(
                total, request.currency(), request.provider(), null, null);
        PaymentIntentResponse intent = paymentService.create(merchantId, intentRequest, null);

        Instant expiresAt = Instant.now().plus(
                gatewayProperties.checkout().sessionTtlMinutes(), ChronoUnit.MINUTES);

        CheckoutSession session = CheckoutSession.builder()
                .merchantId(merchantId)
                .paymentIntentId(intent.id())
                .successUrl(request.successUrl())
                .cancelUrl(request.cancelUrl())
                .expiresAt(expiresAt)
                .status(CheckoutStatus.CREATED)
                .lineItems(jsonUtil.toJson(request.lineItems()))
                .build();
        session = checkoutSessionRepository.save(session);
        metrics.checkoutSessionCreated();
        log.info("Created checkout session {} for merchant {} total {} {}",
                session.getId(), merchantId, total, request.currency());

        return new CheckoutSessionResponse(
                session.getId(), intent.id(), checkoutUrl(session.getId()),
                session.getStatus().name(), total, request.currency(), expiresAt);
    }

    @Transactional
    public CheckoutSession getForDisplay(UUID sessionId) {
        CheckoutSession session = checkoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));
        if (session.getStatus() == CheckoutStatus.CREATED && Instant.now().isAfter(session.getExpiresAt())) {
            session.setStatus(CheckoutStatus.EXPIRED);
            checkoutSessionRepository.save(session);
        }
        return session;
    }

    @Transactional(readOnly = true)
    public String currencyOf(CheckoutSession session) {
        return paymentIntentRepository.findById(session.getPaymentIntentId())
                .map(p -> p.getCurrency())
                .orElse("");
    }

    public List<LineItem> parseLineItems(CheckoutSession session) {
        if (session.getLineItems() == null) {
            return List.of();
        }
        return jsonUtil.fromJson(session.getLineItems(),
                new com.fasterxml.jackson.core.type.TypeReference<List<LineItem>>() {
                });
    }

    /**
     * Called from the hosted checkout page on submit. Confirms the payment and
     * returns the URL to redirect the buyer to.
     */
    @Transactional
    public String complete(UUID sessionId) {
        CheckoutSession session = checkoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));

        if (session.getStatus() == CheckoutStatus.COMPLETED) {
            return session.getSuccessUrl();
        }
        if (Instant.now().isAfter(session.getExpiresAt())) {
            session.setStatus(CheckoutStatus.EXPIRED);
            checkoutSessionRepository.save(session);
            throw new BusinessException("SESSION_EXPIRED", "Checkout session has expired");
        }

        PaymentIntentResponse result = paymentService.confirm(session.getMerchantId(), session.getPaymentIntentId());
        if ("SUCCESS".equals(result.status())) {
            session.setStatus(CheckoutStatus.COMPLETED);
            checkoutSessionRepository.save(session);
            metrics.checkoutSessionCompleted();
            return session.getSuccessUrl();
        }
        return session.getCancelUrl();
    }

    private String checkoutUrl(UUID sessionId) {
        return gatewayProperties.webhook().baseUrl() + "/checkout/" + sessionId;
    }
}
