package com.paygateway.controller;

import com.paygateway.auth.AuthContext;
import com.paygateway.dto.ApiResponse;
import com.paygateway.dto.checkout.CheckoutSessionResponse;
import com.paygateway.dto.checkout.CreateCheckoutSessionRequest;
import com.paygateway.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Checkout")
@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @Operation(summary = "Create a hosted checkout session")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<CheckoutSessionResponse>> create(
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(checkoutService.create(AuthContext.currentMerchantId(), request)));
    }
}
