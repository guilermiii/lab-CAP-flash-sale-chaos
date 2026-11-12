package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.dto.ChaosScenarioType;
import com.chaoslab.flashsale.dto.ChaosStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChaosService {

    private static final Logger log = LoggerFactory.getLogger(ChaosService.class);

    private final ToxiproxyClient toxiproxyClient;

    private static final String POSTGRES_PROXY = "postgres-proxy";
    private static final String REDIS_PROXY = "redis-proxy";

    private volatile String currentScenario = "HEALTHY";

    public ChaosService(ToxiproxyClient toxiproxyClient) {
        this.toxiproxyClient = toxiproxyClient;
    }

    public ChaosStatusResponse applyScenario(ChaosScenarioType scenarioType) {
        log.info("Applying chaos scenario: {}", scenarioType);

        switch (scenarioType) {
            case HEALTHY -> {
                toxiproxyClient.resetProxy(POSTGRES_PROXY);
                toxiproxyClient.resetProxy(REDIS_PROXY);
                toxiproxyClient.setProxyEnabled(POSTGRES_PROXY, true);
                toxiproxyClient.setProxyEnabled(REDIS_PROXY, true);
                currentScenario = "HEALTHY";
                return new ChaosStatusResponse("HEALTHY", true, true, "Network is 100% operational");
            }
            case HIGH_LATENCY -> {
                toxiproxyClient.resetProxy(POSTGRES_PROXY);
                toxiproxyClient.setProxyEnabled(POSTGRES_PROXY, true);
                toxiproxyClient.addLatencyToxic(POSTGRES_PROXY, 3000, 500);
                currentScenario = "HIGH_LATENCY";
                return new ChaosStatusResponse("HIGH_LATENCY", true, true, "PostgreSQL latency injected (+3000ms)");
            }
            case PARTITION_CP -> {
                toxiproxyClient.setProxyEnabled(POSTGRES_PROXY, false);
                toxiproxyClient.setProxyEnabled(REDIS_PROXY, true);
                currentScenario = "PARTITION_CP";
                return new ChaosStatusResponse("PARTITION_CP", false, true, "PostgreSQL network partitioned (CP active)");
            }
            case PARTITION_AP_LAG -> {
                toxiproxyClient.setProxyEnabled(POSTGRES_PROXY, true);
                toxiproxyClient.setProxyEnabled(REDIS_PROXY, false);
                currentScenario = "PARTITION_AP_LAG";
                return new ChaosStatusResponse("PARTITION_AP_LAG", true, false, "Redis network partitioned (AP drift active)");
            }
            default -> throw new IllegalArgumentException("Unknown chaos scenario: " + scenarioType);
        }
    }

    public ChaosStatusResponse getStatus() {
        boolean postgresHealthy = toxiproxyClient.isProxyEnabled(POSTGRES_PROXY);
        boolean redisHealthy = toxiproxyClient.isProxyEnabled(REDIS_PROXY);

        String scenario = currentScenario;
        if (!postgresHealthy && redisHealthy) {
            scenario = "PARTITION_CP";
        } else if (postgresHealthy && !redisHealthy) {
            scenario = "PARTITION_AP_LAG";
        } else if (postgresHealthy && redisHealthy && "HEALTHY".equals(currentScenario)) {
            scenario = "HEALTHY";
        }

        return new ChaosStatusResponse(
                scenario,
                postgresHealthy,
                redisHealthy,
                String.format("PostgreSQL healthy: %s, Redis healthy: %s", postgresHealthy, redisHealthy)
        );
    }
}
