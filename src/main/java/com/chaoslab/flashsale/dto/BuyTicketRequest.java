package com.chaoslab.flashsale.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyTicketRequest(
        @NotBlank(message = "buyerId is required")
        String buyerId
) {
}
