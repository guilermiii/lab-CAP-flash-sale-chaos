package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.dto.ChaosScenarioType;
import com.chaoslab.flashsale.dto.ChaosStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChaosService Unit Tests (Toxiproxy Chaos Engine Orchestrator)")
class ChaosServiceTest {

    @Mock
    private ToxiproxyClient toxiproxyClient;

    @InjectMocks
    private ChaosService chaosService;

    private static final String POSTGRES_PROXY = "postgres-proxy";
    private static final String REDIS_PROXY = "redis-proxy";

    @Nested
    @DisplayName("US04 - Chaos Scenario Switching")
    class ScenarioSwitchingTests {

        @Test
        @DisplayName("Should apply HEALTHY scenario by clearing all toxics and enabling all proxies")
        void shouldApplyHealthyScenario() {
            // Act
            ChaosStatusResponse response = chaosService.applyScenario(ChaosScenarioType.HEALTHY);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.activeScenario()).isEqualTo("HEALTHY");
            assertThat(response.postgresHealthy()).isTrue();
            assertThat(response.redisHealthy()).isTrue();

            then(toxiproxyClient).should().resetProxy(POSTGRES_PROXY);
            then(toxiproxyClient).should().resetProxy(REDIS_PROXY);
            then(toxiproxyClient).should().setProxyEnabled(POSTGRES_PROXY, true);
            then(toxiproxyClient).should().setProxyEnabled(REDIS_PROXY, true);
        }

        @Test
        @DisplayName("Should apply HIGH_LATENCY scenario by injecting 3000ms latency to PostgreSQL proxy")
        void shouldApplyHighLatencyScenario() {
            // Act
            ChaosStatusResponse response = chaosService.applyScenario(ChaosScenarioType.HIGH_LATENCY);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.activeScenario()).isEqualTo("HIGH_LATENCY");
            assertThat(response.postgresHealthy()).isTrue();
            assertThat(response.redisHealthy()).isTrue();

            then(toxiproxyClient).should().resetProxy(POSTGRES_PROXY);
            then(toxiproxyClient).should().setProxyEnabled(POSTGRES_PROXY, true);
            then(toxiproxyClient).should().addLatencyToxic(POSTGRES_PROXY, 3000, 500);
        }

        @Test
        @DisplayName("Should apply PARTITION_CP scenario by cutting PostgreSQL network proxy")
        void shouldApplyPartitionCpScenario() {
            // Act
            ChaosStatusResponse response = chaosService.applyScenario(ChaosScenarioType.PARTITION_CP);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.activeScenario()).isEqualTo("PARTITION_CP");
            assertThat(response.postgresHealthy()).isFalse();
            assertThat(response.redisHealthy()).isTrue();

            then(toxiproxyClient).should().setProxyEnabled(POSTGRES_PROXY, false);
            then(toxiproxyClient).should().setProxyEnabled(REDIS_PROXY, true);
        }

        @Test
        @DisplayName("Should apply PARTITION_AP_LAG scenario by cutting Redis cache sync proxy")
        void shouldApplyPartitionApLagScenario() {
            // Act
            ChaosStatusResponse response = chaosService.applyScenario(ChaosScenarioType.PARTITION_AP_LAG);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.activeScenario()).isEqualTo("PARTITION_AP_LAG");
            assertThat(response.postgresHealthy()).isTrue();
            assertThat(response.redisHealthy()).isFalse();

            then(toxiproxyClient).should().setProxyEnabled(POSTGRES_PROXY, true);
            then(toxiproxyClient).should().setProxyEnabled(REDIS_PROXY, false);
        }
    }

    @Nested
    @DisplayName("US04 - Chaos Status Reporting")
    class StatusReportingTests {

        @Test
        @DisplayName("Should query proxy health and return active chaos status")
        void shouldReturnActiveChaosStatus() {
            // Arrange
            given(toxiproxyClient.isProxyEnabled(POSTGRES_PROXY)).willReturn(true);
            given(toxiproxyClient.isProxyEnabled(REDIS_PROXY)).willReturn(false);

            // Act
            ChaosStatusResponse response = chaosService.getStatus();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.postgresHealthy()).isTrue();
            assertThat(response.redisHealthy()).isFalse();
        }
    }
}
