package com.paygateway.repository.projection;

import com.paygateway.entity.enums.PaymentStatus;

public interface StatusCount {
    PaymentStatus getStatus();

    long getCount();
}
