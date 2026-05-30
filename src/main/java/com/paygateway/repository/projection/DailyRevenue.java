package com.paygateway.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenue {
    LocalDate getDay();

    BigDecimal getRevenue();
}
