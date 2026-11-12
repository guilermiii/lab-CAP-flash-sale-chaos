package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.dto.TelemetryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TelemetryService Unit Tests")
class TelemetryServiceTest {

    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        telemetryService = new TelemetryService();
    }

    @Test
    @DisplayName("Should record transactions and calculate status counters")
    void shouldRecordTransactionsCorrectly() {
        // Act
        telemetryService.recordTransaction("/api/tickets/buy", "POST", 201, 15);
        telemetryService.recordTransaction("/api/tickets/buy", "POST", 201, 12);
        telemetryService.recordTransaction("/api/tickets/buy", "POST", 409, 8);
        telemetryService.recordTransaction("/api/tickets/buy", "POST", 503, 3000);

        TelemetryResponse response = telemetryService.getTelemetry();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.total201()).isEqualTo(2);
        assertThat(response.total409()).isEqualTo(1);
        assertThat(response.total503()).isEqualTo(1);
        assertThat(response.recentTransactions()).hasSize(4);
        assertThat(response.recentTransactions().get(0).statusCode()).isEqualTo(503);
    }

    @Test
    @DisplayName("Should reset all counters and recent transactions")
    void shouldResetCounters() {
        // Arrange
        telemetryService.recordTransaction("/api/tickets/buy", "POST", 201, 15);

        // Act
        telemetryService.reset();
        TelemetryResponse response = telemetryService.getTelemetry();

        // Assert
        assertThat(response.total201()).isEqualTo(0);
        assertThat(response.recentTransactions()).isEmpty();
    }
}
