package com.paygateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is well-formed but violates a business rule
 * (e.g. refund amount exceeds remaining balance, invalid state transition).
 */
public class BusinessException extends ApiException {

    public BusinessException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    public BusinessException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", message);
    }
}
