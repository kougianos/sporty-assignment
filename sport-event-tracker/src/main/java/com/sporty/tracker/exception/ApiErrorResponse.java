package com.sporty.tracker.exception;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ApiErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors
) {
}
