package com.paygateway.dto;

import java.util.Map;

/**
 * Structured error body included in the response envelope.
 */
public record ApiError(String code, String message, Map<String, String> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }
}
