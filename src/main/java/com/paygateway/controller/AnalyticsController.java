package com.paygateway.controller;

import com.paygateway.auth.AuthContext;
import com.paygateway.dto.ApiResponse;
import com.paygateway.dto.analytics.AnalyticsSummaryResponse;
import com.paygateway.dto.analytics.TimeseriesPoint;
import com.paygateway.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Tag(name = "Analytics")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Aggregate volume, success rate, avg value and top currencies")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.summary(AuthContext.currentMerchantId(), from, to)));
    }

    @Operation(summary = "Daily revenue for the last N days")
    @GetMapping("/timeseries")
    public ResponseEntity<ApiResponse<List<TimeseriesPoint>>> timeseries(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.timeseries(AuthContext.currentMerchantId(), days)));
    }
}
