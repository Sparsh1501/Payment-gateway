package com.paygateway.controller;

import com.paygateway.auth.AuthContext;
import com.paygateway.dto.ApiResponse;
import com.paygateway.dto.auth.ApiKeyResponse;
import com.paygateway.dto.auth.MerchantResponse;
import com.paygateway.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Merchant & API Keys")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "Get the authenticated merchant profile")
    @GetMapping("/merchant/profile")
    public ResponseEntity<ApiResponse<MerchantResponse>> profile() {
        return ResponseEntity.ok(ApiResponse.ok(merchantService.getProfile(AuthContext.currentMerchantId())));
    }

    @Operation(summary = "Reveal the merchant API key + secret")
    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> apiKeys() {
        return ResponseEntity.ok(ApiResponse.ok(merchantService.getApiCredentials(AuthContext.currentMerchantId())));
    }

    @Operation(summary = "Regenerate the merchant API key + secret")
    @PostMapping("/api-keys/regenerate")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> regenerate() {
        return ResponseEntity.ok(ApiResponse.ok(
                merchantService.regenerateApiCredentials(AuthContext.currentMerchantId())));
    }
}
