package com.chaoslab.flashsale.dto;

import java.time.Instant;

public record BuyTicketResponse(
        Long orderId,
        String buyerId,
        Instant purchasedAt,
        int remainingStock
) {
}
