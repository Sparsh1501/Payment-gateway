package com.paygateway.service;

import com.paygateway.dto.analytics.AnalyticsSummaryResponse;
import com.paygateway.dto.analytics.TimeseriesPoint;
import com.paygateway.entity.enums.PaymentStatus;
import com.paygateway.repository.PaymentIntentRepository;
import com.paygateway.repository.TransactionRepository;
import com.paygateway.repository.projection.DailyRevenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse summary(UUID merchantId, Instant from, Instant to) {
        Instant effFrom = from != null ? from : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant effTo = to != null ? to : Instant.now();

        Map<PaymentStatus, Long> counts = new HashMap<>();
        paymentIntentRepository.countByStatus(merchantId, effFrom, effTo)
                .forEach(sc -> counts.put(sc.getStatus(), sc.getCount()));

        long success = counts.getOrDefault(PaymentStatus.SUCCESS, 0L);
        long failed = counts.getOrDefault(PaymentStatus.FAILED, 0L);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long attempted = success + failed;
        double successRate = attempted == 0 ? 0.0
                : BigDecimal.valueOf((double) success / attempted * 100)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();

        BigDecimal volume = transactionRepository.totalVolume(merchantId, effFrom, effTo);
        if (volume == null) {
            volume = BigDecimal.ZERO;
        }
        BigDecimal avg = success == 0 ? BigDecimal.ZERO
                : volume.divide(BigDecimal.valueOf(success), 2, RoundingMode.HALF_UP);

        List<AnalyticsSummaryResponse.CurrencyVolumeDto> topCurrencies =
                transactionRepository.volumeByCurrency(merchantId, effFrom, effTo).stream()
                        .map(cv -> new AnalyticsSummaryResponse.CurrencyVolumeDto(cv.getCurrency(), cv.getVolume()))
                        .toList();

        return new AnalyticsSummaryResponse(
                effFrom, effTo, volume, total, success, failed, successRate, avg, topCurrencies);
    }

    @Transactional(readOnly = true)
    public List<TimeseriesPoint> timeseries(UUID merchantId, int days) {
        int window = Math.max(1, Math.min(days, 365));
        Instant from = Instant.now().minus(window, ChronoUnit.DAYS);
        Instant to = Instant.now();

        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (DailyRevenue dr : transactionRepository.revenueByDay(merchantId, from, to)) {
            byDay.put(dr.getDay(), dr.getRevenue());
        }

        LocalDate start = LocalDate.ofInstant(from, ZoneOffset.UTC);
        LocalDate end = LocalDate.ofInstant(to, ZoneOffset.UTC);
        return start.datesUntil(end.plusDays(1))
                .map(d -> new TimeseriesPoint(d, byDay.getOrDefault(d, BigDecimal.ZERO)))
                .toList();
    }
}
