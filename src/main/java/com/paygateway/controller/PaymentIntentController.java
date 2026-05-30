package com.paygateway.controller;

import com.paygateway.auth.AuthContext;
import com.paygateway.dto.ApiResponse;
import com.paygateway.dto.payment.CreatePaymentIntentRequest;
import com.paygateway.dto.payment.PaymentIntentResponse;
import com.paygateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Payment Intents")
@RestController
@RequestMapping("/api/v1/payment-intents")
@RequiredArgsConstructor
public class PaymentIntentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create a payment intent")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> create(
            @Valid @RequestBody CreatePaymentIntentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PaymentIntentResponse response = paymentService.create(
                AuthContext.currentMerchantId(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "Fetch a payment intent by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.get(AuthContext.currentMerchantId(), id)));
    }

    @Operation(summary = "List payment intents (paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentIntentResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(paymentService.list(AuthContext.currentMerchantId(), pageable)));
    }

    @Operation(summary = "Confirm a payment intent and trigger the provider charge")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.confirm(AuthContext.currentMerchantId(), id)));
    }

    @Operation(summary = "Cancel a payment intent")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.cancel(AuthContext.currentMerchantId(), id)));
    }
}
