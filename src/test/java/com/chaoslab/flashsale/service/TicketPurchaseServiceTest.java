package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.domain.TicketInventory;
import com.chaoslab.flashsale.domain.TicketOrder;
import com.chaoslab.flashsale.dto.BuyTicketRequest;
import com.chaoslab.flashsale.dto.BuyTicketResponse;
import com.chaoslab.flashsale.exception.ServiceUnavailableException;
import com.chaoslab.flashsale.exception.SoldOutException;
import com.chaoslab.flashsale.repository.TicketInventoryRepository;
import com.chaoslab.flashsale.repository.TicketOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketPurchaseService Unit Tests (ACID / CP Purchase Flow)")
class TicketPurchaseServiceTest {

    @Mock
    private TicketInventoryRepository inventoryRepository;

    @Mock
    private TicketOrderRepository orderRepository;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private TicketPurchaseService ticketPurchaseService;

    @Nested
    @DisplayName("US01 - Happy Path: Available Stock")
    class HappyPathTests {

        @Test
        @DisplayName("Should successfully purchase ticket, decrement stock in Postgres, save order, and trigger async sync")
        void shouldSuccessfullyPurchaseTicketWhenStockIsAvailable() {
            // Arrange
            TicketInventory inventory = TicketInventory.builder()
                    .id(1L)
                    .totalQuantity(50)
                    .availableQuantity(50)
                    .version(0L)
                    .build();

            TicketOrder savedOrder = TicketOrder.builder()
                    .id(101L)
                    .buyerId("user-42")
                    .purchasedAt(Instant.now())
                    .status("CONFIRMED")
                    .build();

            given(inventoryRepository.findWithLockById(1L)).willReturn(Optional.of(inventory));
            given(orderRepository.save(any(TicketOrder.class))).willReturn(savedOrder);

            BuyTicketRequest request = new BuyTicketRequest("user-42");

            // Act
            BuyTicketResponse response = ticketPurchaseService.buyTicket(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.orderId()).isEqualTo(101L);
            assertThat(response.buyerId()).isEqualTo("user-42");
            assertThat(response.remainingStock()).isEqualTo(49);

            assertThat(inventory.getAvailableQuantity()).isEqualTo(49);
            then(inventoryRepository).should().save(inventory);

            ArgumentCaptor<TicketOrder> orderCaptor = ArgumentCaptor.forClass(TicketOrder.class);
            then(orderRepository).should().save(orderCaptor.capture());
            TicketOrder orderToSave = orderCaptor.getValue();
            assertThat(orderToSave.getBuyerId()).isEqualTo("user-42");
            assertThat(orderToSave.getStatus()).isEqualTo("CONFIRMED");

            then(catalogService).should().syncCatalogAsync(eq(49), eq("user-42"));
        }
    }

    @Nested
    @DisplayName("US01 - Conflict Path: Sold Out Stock")
    class SoldOutTests {

        @Test
        @DisplayName("Should throw SoldOutException when available stock is zero without saving order")
        void shouldThrowSoldOutExceptionWhenStockIsZero() {
            // Arrange
            TicketInventory inventory = TicketInventory.builder()
                    .id(1L)
                    .totalQuantity(50)
                    .availableQuantity(0)
                    .version(10L)
                    .build();

            given(inventoryRepository.findWithLockById(1L)).willReturn(Optional.of(inventory));
            BuyTicketRequest request = new BuyTicketRequest("user-99");

            // Act & Assert
            assertThatThrownBy(() -> ticketPurchaseService.buyTicket(request))
                    .isInstanceOf(SoldOutException.class)
                    .hasMessageContaining("Tickets are sold out");

            then(orderRepository).should(never()).save(any());
            then(catalogService).should(never()).syncCatalogAsync(any(Integer.class), any());
        }
    }

    @Nested
    @DisplayName("US01 - CP / Resilience Path: Database Failure & Partition")
    class ChaosAndPartitionTests {

        @Test
        @DisplayName("Should throw ServiceUnavailableException when database lock cannot be acquired (Toxiproxy partition)")
        void shouldThrowServiceUnavailableWhenLockFails() {
            // Arrange
            given(inventoryRepository.findWithLockById(1L))
                    .willThrow(new CannotAcquireLockException("Lock wait timeout exceeded"));

            BuyTicketRequest request = new BuyTicketRequest("user-1");

            // Act & Assert
            assertThatThrownBy(() -> ticketPurchaseService.buyTicket(request))
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessageContaining("Database unavailable or lock timeout");

            then(orderRepository).should(never()).save(any());
            then(catalogService).should(never()).syncCatalogAsync(any(Integer.class), any());
        }

        @Test
        @DisplayName("Should throw ServiceUnavailableException when database query times out")
        void shouldThrowServiceUnavailableWhenQueryTimesOut() {
            // Arrange
            given(inventoryRepository.findWithLockById(1L))
                    .willThrow(new QueryTimeoutException("Connection timed out"));

            BuyTicketRequest request = new BuyTicketRequest("user-2");

            // Act & Assert
            assertThatThrownBy(() -> ticketPurchaseService.buyTicket(request))
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessageContaining("Database unavailable or lock timeout");
        }

        @Test
        @DisplayName("Should throw ServiceUnavailableException when inventory record is not initialized")
        void shouldThrowServiceUnavailableWhenInventoryNotFound() {
            // Arrange
            given(inventoryRepository.findWithLockById(1L)).willReturn(Optional.empty());
            BuyTicketRequest request = new BuyTicketRequest("user-3");

            // Act & Assert
            assertThatThrownBy(() -> ticketPurchaseService.buyTicket(request))
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessageContaining("Inventory not found or uninitialized");
        }
    }
}
