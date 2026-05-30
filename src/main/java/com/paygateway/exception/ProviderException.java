package com.paygateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Wraps a failure returned by (or while communicating with) a payment provider.
 */
public class ProviderException extends ApiException {

    public ProviderException(String message) {
        super(HttpStatus.BAD_GATEWAY, "PROVIDER_ERROR", message);
    }

    public ProviderException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "PROVIDER_ERROR", message);
        initCause(cause);
    }
}
