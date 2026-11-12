package com.chaoslab.flashsale.dto;

public record TelemetryRecord(
        String id,
        String timestamp,
        String endpoint,
        String method,
        int statusCode,
        long durationMs
) {
}
