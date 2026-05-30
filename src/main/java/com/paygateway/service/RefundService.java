package com.paygateway.service;

import com.paygateway.dto.refund.CreateRefundRequest;
import com.paygateway.dto.refund.RefundResponse;
import com.paygateway.entity.PaymentIntent;
import com.paygateway.entity.Refund;
import com.paygateway.entity.Transaction;
import com.paygateway.entity.enums.PaymentStatus;
import com.paygateway.entity.enums.RefundStatus;
import com.paygateway.entity.enums.TransactionType;
import com.paygateway.exception.BusinessException;
import com.paygateway.exception.ResourceNotFoundException;
import com.paygateway.provider.ProviderGateway;
import com.paygateway.provider.RefundRequest;
import com.paygateway.provider.RefundResult;
import com.paygateway.repository.PaymentIntentRepository;
import com.paygateway.repository.RefundRepository;
import com.paygateway.repository.TransactionRepository;
import com.paygateway.webhook.WebhookEvents;
import com.paygateway.webhook.WebhookPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final TransactionRepository transactionRepository;
    private final ProviderGateway providerGateway;
    private final WebhookPublisher webhookPublisher;

    @Transactional
    public RefundResponse create(UUID merchantId, CreateRefundRequest request) {
        PaymentIntent intent = paymentIntentRepository
                .findByIdAndMerchantId(request.paymentIntentId(), merchantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment intent not found: " + request.paymentIntentId()));

        if (intent.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException("PAYMENT_NOT_REFUNDABLE",
                    "Only successful payments can be refunded (current: " + intent.getStatus() + ")");
        }

        BigDecimal alreadyRefunded = refundRepository.sumRefundedAmount(
                intent.getId(), List.of(RefundStatus.SUCCESS, RefundStatus.PENDING));
        BigDecimal remaining = intent.getAmount().subtract(alreadyRefunded);
        if (request.amount().compareTo(remaining) > 0) {
            throw new BusinessException("REFUND_EXCEEDS_BALANCE",
                    "Refund amount " + request.amount() + " exceeds refundable balance " + remaining);
        }

        Refund refund = Refund.builder()
                .paymentIntentId(intent.getId())
                .merchantId(merchantId)
                .amount(request.amount())
                .reason(request.reason())
                .status(RefundStatus.PENDING)
                .build();
        refund = refundRepository.save(refund);
        log.info("Created refund {} amount {} for payment-intent {}",
                refund.getId(), refund.getAmount(), intent.getId());
        webhookPublisher.publish(merchantId, WebhookEvents.REFUND_CREATED, RefundResponse.from(refund));

        RefundRequest providerRequest = new RefundRequest(
                refund.getId(), intent.getProviderPaymentId(),
                refund.getAmount(), intent.getCurrency(), refund.getReason());

        try {
            RefundResult result = providerGateway.refund(intent.getProvider(), providerRequest).join();
            if (result.success()) {
                refund.setStatus(RefundStatus.SUCCESS);
                refund.setProviderRefundId(result.providerRefundId());
                recordRefundTransaction(intent, refund);
                webhookPublisher.publish(merchantId, WebhookEvents.REFUND_SUCCESS, RefundResponse.from(refund));
            } else {
                refund.setStatus(RefundStatus.FAILED);
                webhookPublisher.publish(merchantId, WebhookEvents.REFUND_FAILED, RefundResponse.from(refund));
            }
        } catch (Exception e) {
            log.warn("Refund {} failed: {}", refund.getId(), e.getMessage());
            refund.setStatus(RefundStatus.FAILED);
            webhookPublisher.publish(merchantId, WebhookEvents.REFUND_FAILED, RefundResponse.from(refund));
        }
        refundRepository.save(refund);
        return RefundResponse.from(refund);
    }

    @Transactional(readOnly = true)
    public RefundResponse get(UUID merchantId, UUID id) {
        return refundRepository.findByIdAndMerchantId(id, merchantId)
                .map(RefundResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<RefundResponse> list(UUID merchantId, Pageable pageable) {
        return refundRepository.findByMerchantId(merchantId, pageable).map(RefundResponse::from);
    }

    private void recordRefundTransaction(PaymentIntent intent, Refund refund) {
        Transaction txn = Transaction.builder()
                .paymentIntentId(intent.getId())
                .type(TransactionType.REFUND)
                .amount(refund.getAmount())
                .currency(intent.getCurrency())
                .fee(BigDecimal.ZERO)
                .net(refund.getAmount().negate())
                .provider(intent.getProvider())
                .build();
        transactionRepository.save(txn);
    }
}
