package com.paygateway.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeseriesPoint(LocalDate date, BigDecimal revenue) {
}
