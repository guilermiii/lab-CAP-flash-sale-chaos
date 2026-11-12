package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.domain.TicketInventory;
import com.chaoslab.flashsale.dto.CatalogResponse;
import com.chaoslab.flashsale.repository.TicketInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final StringRedisTemplate redisTemplate;
    private final TicketInventoryRepository inventoryRepository;

    private static final String REDIS_STOCK_KEY = "ticket:catalog:stock";
    private static final String REDIS_RECENT_BUYERS_KEY = "ticket:catalog:recent_buyers";
    private static final Long DEFAULT_INVENTORY_ID = 1L;

    public CatalogService(StringRedisTemplate redisTemplate, TicketInventoryRepository inventoryRepository) {
        this.redisTemplate = redisTemplate;
        this.inventoryRepository = inventoryRepository;
    }

    public CatalogResponse getCatalog() {
        int redisStock;
        List<String> recentBuyers;

        try {
            String stockStr = redisTemplate.opsForValue().get(REDIS_STOCK_KEY);
            redisStock = stockStr != null ? Integer.parseInt(stockStr) : 0;
            List<String> buyers = redisTemplate.opsForList().range(REDIS_RECENT_BUYERS_KEY, 0, 9);
            recentBuyers = buyers != null ? buyers : List.of();
        } catch (Exception ex) {
            log.error("Failed to read catalog from Redis cache: {}", ex.getMessage());
            return new CatalogResponse(-1, List.of(), "UNAVAILABLE", false, 0);
        }

        // Tenta consultar a verdade real no PostgreSQL para calcular Drift (BASE)
        try {
            TicketInventory postgresInventory = inventoryRepository.findById(DEFAULT_INVENTORY_ID)
                    .orElse(null);

            if (postgresInventory != null) {
                int postgresStock = postgresInventory.getAvailableQuantity();
                int driftAmount = Math.abs(postgresStock - redisStock);
                boolean driftDetected = driftAmount > 0;
                return new CatalogResponse(redisStock, recentBuyers, "REDIS", driftDetected, driftAmount);
            }

            return new CatalogResponse(redisStock, recentBuyers, "REDIS", false, 0);
        } catch (Exception ex) {
            // Se o PostgreSQL estiver particionado pelo Toxiproxy, a vitrine continua servindo do Redis (Garantia AP)
            log.warn("PostgreSQL unavailable when reading catalog truth; serving stale cache (AP mode): {}", ex.getMessage());
            return new CatalogResponse(redisStock, recentBuyers, "REDIS_FALLBACK_AP", false, 0);
        }
    }

    @Async
    public void syncCatalogAsync(int currentStock, String buyerId) {
        try {
            // Simula um replication lag leve (configurável/visível)
            Thread.sleep(100);
            redisTemplate.opsForValue().set(REDIS_STOCK_KEY, String.valueOf(currentStock));
            redisTemplate.opsForList().leftPush(REDIS_RECENT_BUYERS_KEY, buyerId);
            redisTemplate.opsForList().trim(REDIS_RECENT_BUYERS_KEY, 0, 9);
            log.info("Catalog synced to Redis: stock={}, latestBuyer={}", currentStock, buyerId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("Failed to propagate async catalog sync to Redis: {}", ex.getMessage());
        }
    }

    public void resetCatalog(int initialStock) {
        try {
            redisTemplate.opsForValue().set(REDIS_STOCK_KEY, String.valueOf(initialStock));
            redisTemplate.delete(REDIS_RECENT_BUYERS_KEY);
            log.info("Catalog reset in Redis to {}", initialStock);
        } catch (Exception ex) {
            log.warn("Failed to reset Redis catalog: {}", ex.getMessage());
        }
    }
}
