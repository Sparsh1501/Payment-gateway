package com.paygateway.controller;

import com.paygateway.dto.ApiResponse;
import com.paygateway.webhook.InboundWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Inbound Provider Webhooks")
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderWebhookController {

    private final InboundWebhookService inboundWebhookService;

    @Operation(summary = "Receive Stripe webhook events")
    @PostMapping("/stripe/webhook")
    public ResponseEntity<ApiResponse<Map<String, String>>> stripe(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        inboundWebhookService.handleStripe(payload, signature);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("received", "true")));
    }

    @Operation(summary = "Receive Razorpay webhook events")
    @PostMapping("/razorpay/webhook")
    public ResponseEntity<ApiResponse<Map<String, String>>> razorpay(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        inboundWebhookService.handleRazorpay(payload, signature);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("received", "true")));
    }
}
