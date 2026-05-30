package com.paygateway.controller;

import com.paygateway.auth.AuthContext;
import com.paygateway.dto.ApiResponse;
import com.paygateway.dto.webhook.CreateWebhookEndpointRequest;
import com.paygateway.dto.webhook.WebhookDeliveryResponse;
import com.paygateway.dto.webhook.WebhookEndpointResponse;
import com.paygateway.service.WebhookEndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Webhooks")
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookEndpointService webhookEndpointService;

    @Operation(summary = "Register a webhook endpoint")
    @PostMapping("/endpoints")
    public ResponseEntity<ApiResponse<WebhookEndpointResponse>> create(
            @Valid @RequestBody CreateWebhookEndpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(webhookEndpointService.create(AuthContext.currentMerchantId(), request)));
    }

    @Operation(summary = "List webhook endpoints")
    @GetMapping("/endpoints")
    public ResponseEntity<ApiResponse<List<WebhookEndpointResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(webhookEndpointService.list(AuthContext.currentMerchantId())));
    }

    @Operation(summary = "Delete a webhook endpoint")
    @DeleteMapping("/endpoints/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        webhookEndpointService.delete(AuthContext.currentMerchantId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "List webhook delivery attempts")
    @GetMapping("/deliveries")
    public ResponseEntity<ApiResponse<Page<WebhookDeliveryResponse>>> deliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(
                webhookEndpointService.listDeliveries(AuthContext.currentMerchantId(), pageable)));
    }
}
