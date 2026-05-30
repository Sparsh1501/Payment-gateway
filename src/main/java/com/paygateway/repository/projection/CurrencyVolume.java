package com.paygateway.repository.projection;

import java.math.BigDecimal;

public interface CurrencyVolume {
    String getCurrency();

    BigDecimal getVolume();
}
