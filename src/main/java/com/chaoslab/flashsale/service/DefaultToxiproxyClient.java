package com.chaoslab.flashsale.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class DefaultToxiproxyClient implements ToxiproxyClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultToxiproxyClient.class);

    private final RestClient restClient;

    public DefaultToxiproxyClient(@Value("${toxiproxy.api-url:http://localhost:8474}") String apiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    @Override
    public void setProxyEnabled(String proxyName, boolean enabled) {
        try {
            restClient.post()
                    .uri("/proxies/{name}", proxyName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", enabled))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Toxiproxy: set proxy '{}' enabled={}", proxyName, enabled);
        } catch (Exception ex) {
            log.error("Failed to set enabled={} for Toxiproxy proxy '{}': {}", enabled, proxyName, ex.getMessage());
        }
    }

    @Override
    public void addLatencyToxic(String proxyName, int latencyMs, int jitterMs) {
        try {
            Map<String, Object> toxicPayload = Map.of(
                    "name", "latency_toxic",
                    "type", "latency",
                    "stream", "downstream",
                    "toxicity", 1.0,
                    "attributes", Map.of(
                            "latency", latencyMs,
                            "jitter", jitterMs
                    )
            );

            restClient.post()
                    .uri("/proxies/{name}/toxics", proxyName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toxicPayload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Toxiproxy: added latency toxic to '{}' (latency={}ms, jitter={}ms)", proxyName, latencyMs, jitterMs);
        } catch (Exception ex) {
            log.error("Failed to add latency toxic to Toxiproxy proxy '{}': {}", proxyName, ex.getMessage());
        }
    }

    @Override
    public void resetProxy(String proxyName) {
        try {
            // Remove toxic específico se existir
            restClient.delete()
                    .uri("/proxies/{name}/toxics/latency_toxic", proxyName)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Toxiproxy: reset/removed toxics for proxy '{}'", proxyName);
        } catch (Exception ex) {
            log.debug("No toxic to remove or reset failed for '{}': {}", proxyName, ex.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isProxyEnabled(String proxyName) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/proxies/{name}", proxyName)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("enabled")) {
                return Boolean.TRUE.equals(response.get("enabled"));
            }
            return false;
        } catch (Exception ex) {
            log.warn("Failed to check status of Toxiproxy proxy '{}': {}", proxyName, ex.getMessage());
            return false;
        }
    }
}
