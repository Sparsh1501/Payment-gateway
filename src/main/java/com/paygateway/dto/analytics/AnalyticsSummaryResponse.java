package com.paygateway.dto.analytics;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AnalyticsSummaryResponse(
        Instant from,
        Instant to,
        BigDecimal totalVolume,
        long totalTransactions,
        long successCount,
        long failedCount,
        double successRate,
        BigDecimal avgTransactionValue,
        List<CurrencyVolumeDto> topCurrencies
) {
    public record CurrencyVolumeDto(String currency, BigDecimal volume) {
    }
}
