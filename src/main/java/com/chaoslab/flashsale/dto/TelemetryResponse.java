package com.chaoslab.flashsale.dto;

import java.util.List;

public record TelemetryResponse(
        double rps,
        int total201,
        int total409,
        int total503,
        int totalOthers,
        List<TelemetryRecord> recentTransactions
) {
}
