package com.chaoslab.flashsale.dto;

import java.time.Instant;

public record ResetResponse(
        int stockResetTo,
        String message,
        Instant resetAt
) {
}
