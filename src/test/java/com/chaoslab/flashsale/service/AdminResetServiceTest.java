package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.domain.TicketInventory;
import com.chaoslab.flashsale.dto.ChaosScenarioType;
import com.chaoslab.flashsale.dto.ChaosStatusResponse;
import com.chaoslab.flashsale.dto.ResetResponse;
import com.chaoslab.flashsale.repository.TicketInventoryRepository;
import com.chaoslab.flashsale.repository.TicketOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminResetService Unit Tests (Environment Reset for Demos)")
class AdminResetServiceTest {

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private ChaosService chaosService;

    @InjectMocks
    private AdminResetService adminResetService;

    @Test
    @DisplayName("Should reset inventory to 50, clear orders, reset Redis catalog, and restore Toxiproxy to HEALTHY")
    void shouldResetEnvironmentTo50TicketsAndHealthyNetwork() {
        // Arrange
        TicketInventory existingInventory = TicketInventory.builder()
                .id(1L)
                .totalQuantity(50)
                .availableQuantity(0)
                .version(50L)
                .build();
        given(inventoryRepository.findById(1L)).willReturn(Optional.of(existingInventory));

        given(chaosService.applyScenario(ChaosScenarioType.HEALTHY))
                .willReturn(new ChaosStatusResponse("HEALTHY", true, true, "All proxies operational"));

        // Act
        ResetResponse response = adminResetService.resetEnvironment();

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.stockResetTo()).isEqualTo(50);
        assertThat(response.message()).contains("Environment reset successfully");

        // Verify order cleanup
        then(orderRepository).should().deleteAll();

        // Verify inventory reset to 50
        ArgumentCaptor<TicketInventory> inventoryCaptor = ArgumentCaptor.forClass(TicketInventory.class);
        then(inventoryRepository).should().save(inventoryCaptor.capture());
        TicketInventory savedInventory = inventoryCaptor.getValue();
        assertThat(savedInventory.getId()).isEqualTo(1L);
        assertThat(savedInventory.getTotalQuantity()).isEqualTo(50);
        assertThat(savedInventory.getAvailableQuantity()).isEqualTo(50);

        // Verify Redis catalog reset
        then(catalogService).should().resetCatalog(50);

        // Verify Toxiproxy restored to HEALTHY
        then(chaosService).should().applyScenario(ChaosScenarioType.HEALTHY);
    }
}
