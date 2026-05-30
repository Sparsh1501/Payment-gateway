package com.paygateway.service;

import com.paygateway.config.props.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes gateway processing fees: {@code fee% * amount + flat}.
 */
@Component
@RequiredArgsConstructor
public class FeeCalculator {

    private final GatewayProperties properties;

    public BigDecimal fee(BigDecimal amount) {
        BigDecimal percentage = properties.feePercentage().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return amount.multiply(percentage)
                .add(properties.feeFlat())
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal net(BigDecimal amount) {
        return amount.subtract(fee(amount)).setScale(2, RoundingMode.HALF_UP);
    }
}
