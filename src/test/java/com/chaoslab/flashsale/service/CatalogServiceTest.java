package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.domain.TicketInventory;
import com.chaoslab.flashsale.dto.CatalogResponse;
import com.chaoslab.flashsale.repository.TicketInventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Unit Tests (BASE / AP Catalog Flow)")
class CatalogServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Nested
    @DisplayName("US02 - Vitrine Read Operations")
    class CatalogReadTests {

        @Test
        @DisplayName("Should return catalog from Redis with zero drift when Redis matches Postgres")
        void shouldReturnCatalogFromRedisCacheWithoutDrift() {
            // Arrange
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForList()).willReturn(listOperations);
            given(valueOperations.get("ticket:catalog:stock")).willReturn("45");
            given(listOperations.range("ticket:catalog:recent_buyers", 0, 9))
                    .willReturn(List.of("user-1", "user-2"));

            TicketInventory postgresInventory = TicketInventory.builder()
                    .id(1L)
                    .availableQuantity(45)
                    .build();
            given(inventoryRepository.findById(1L)).willReturn(Optional.of(postgresInventory));

            // Act
            CatalogResponse response = catalogService.getCatalog();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.availableStock()).isEqualTo(45);
            assertThat(response.recentBuyers()).containsExactly("user-1", "user-2");
            assertThat(response.dataSource()).isEqualTo("REDIS");
            assertThat(response.driftDetected()).isFalse();
            assertThat(response.driftAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should detect drift (delta > 0) when Redis cache is stale compared to PostgreSQL")
        void shouldDetectDriftWhenCacheDiffersFromPostgres() {
            // Arrange
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForList()).willReturn(listOperations);
            // Redis says 50 tickets remain (stale cache due to lag or partition)
            given(valueOperations.get("ticket:catalog:stock")).willReturn("50");
            given(listOperations.range("ticket:catalog:recent_buyers", 0, 9)).willReturn(List.of());

            // Postgres truth says only 30 tickets remain
            TicketInventory postgresInventory = TicketInventory.builder()
                    .id(1L)
                    .availableQuantity(30)
                    .build();
            given(inventoryRepository.findById(1L)).willReturn(Optional.of(postgresInventory));

            // Act
            CatalogResponse response = catalogService.getCatalog();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.availableStock()).isEqualTo(50);
            assertThat(response.driftDetected()).isTrue();
            assertThat(response.driftAmount()).isEqualTo(20);
        }

        @Test
        @DisplayName("Should maintain AP availability from Redis even when PostgreSQL is partitioned or down")
        void shouldMaintainAvailabilityWhenPostgresIsDown() {
            // Arrange
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForList()).willReturn(listOperations);
            given(valueOperations.get("ticket:catalog:stock")).willReturn("40");
            given(listOperations.range("ticket:catalog:recent_buyers", 0, 9)).willReturn(List.of("user-9"));

            // Postgres throws connection exception due to network partition
            given(inventoryRepository.findById(1L))
                    .willThrow(new RuntimeException("Postgres connection partitioned via Toxiproxy"));

            // Act
            CatalogResponse response = catalogService.getCatalog();

            // Assert: AP availability preserved, response returned with Redis cache
            assertThat(response).isNotNull();
            assertThat(response.availableStock()).isEqualTo(40);
            assertThat(response.dataSource()).isEqualTo("REDIS_FALLBACK_AP");
            assertThat(response.recentBuyers()).containsExactly("user-9");
        }

        @Test
        @DisplayName("Should return degraded catalog response when Redis is partitioned or unavailable")
        void shouldDegradeGracefullyWhenRedisIsUnavailable() {
            // Arrange
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get(anyString()))
                    .willThrow(new RedisConnectionFailureException("Redis connection partitioned via Toxiproxy"));

            // Act
            CatalogResponse response = catalogService.getCatalog();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.dataSource()).isEqualTo("UNAVAILABLE");
            assertThat(response.availableStock()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("US02 - Async Sync & Reset Operations")
    class CatalogSyncTests {

        @Test
        @DisplayName("Should sync catalog asynchronously updating Redis stock and prepending recent buyer")
        void shouldSyncCatalogAsync() {
            // Arrange
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForList()).willReturn(listOperations);

            // Act
            catalogService.syncCatalogAsync(48, "user-55");

            // Assert
            then(valueOperations).should().set("ticket:catalog:stock", "48");
            then(listOperations).should().leftPush("ticket:catalog:recent_buyers", "user-55");
            then(listOperations).should().trim("ticket:catalog:recent_buyers", 0, 9);
        }

        @Test
        @DisplayName("Should reset catalog in Redis and clear recent buyers list")
        void shouldResetCatalog() {
            // Arrange
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // Act
            catalogService.resetCatalog(50);

            // Assert
            then(valueOperations).should().set("ticket:catalog:stock", "50");
            then(redisTemplate).should().delete("ticket:catalog:recent_buyers");
        }
    }
}
