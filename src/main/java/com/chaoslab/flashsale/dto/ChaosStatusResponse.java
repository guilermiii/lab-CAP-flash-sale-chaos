package com.chaoslab.flashsale.dto;

public record ChaosStatusResponse(
        String activeScenario,
        boolean postgresHealthy,
        boolean redisHealthy,
        String description
) {
}
