package com.paygateway.controller;

import com.paygateway.auth.AuthContext;
import com.paygateway.dto.ApiResponse;
import com.paygateway.dto.refund.CreateRefundRequest;
import com.paygateway.dto.refund.RefundResponse;
import com.paygateway.service.RefundService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Refunds")
@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "Create a (full or partial) refund")
    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> create(@Valid @RequestBody CreateRefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(refundService.create(AuthContext.currentMerchantId(), request)));
    }

    @Operation(summary = "Fetch a refund by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RefundResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.get(AuthContext.currentMerchantId(), id)));
    }

    @Operation(summary = "List refunds (paginated)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(refundService.list(AuthContext.currentMerchantId(), pageable)));
    }
}
