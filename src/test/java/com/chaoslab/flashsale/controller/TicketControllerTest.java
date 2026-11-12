package com.chaoslab.flashsale.controller;

import com.chaoslab.flashsale.dto.BuyTicketRequest;
import com.chaoslab.flashsale.dto.BuyTicketResponse;
import com.chaoslab.flashsale.dto.CatalogResponse;
import com.chaoslab.flashsale.exception.GlobalExceptionHandler;
import com.chaoslab.flashsale.exception.ServiceUnavailableException;
import com.chaoslab.flashsale.exception.SoldOutException;
import com.chaoslab.flashsale.service.CatalogService;
import com.chaoslab.flashsale.service.TicketPurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketController Standalone MockMvc Tests")
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketPurchaseService ticketPurchaseService;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private TicketController ticketController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/tickets/buy")
    class BuyTicketEndpointTests {

        @Test
        @DisplayName("Should return 201 Created and ticket order details when purchase succeeds")
        void shouldReturn201WhenPurchaseSuccessful() throws Exception {
            // Arrange
            BuyTicketRequest request = new BuyTicketRequest("user-100");
            BuyTicketResponse response = new BuyTicketResponse(1L, "user-100", Instant.now(), 49);

            given(ticketPurchaseService.buyTicket(any(BuyTicketRequest.class))).willReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/tickets/buy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").value(1))
                    .andExpect(jsonPath("$.buyerId").value("user-100"))
                    .andExpect(jsonPath("$.remainingStock").value(49));
        }

        @Test
        @DisplayName("Should return 409 Conflict when tickets are sold out")
        void shouldReturn409ConflictWhenSoldOut() throws Exception {
            // Arrange
            BuyTicketRequest request = new BuyTicketRequest("user-101");
            given(ticketPurchaseService.buyTicket(any(BuyTicketRequest.class)))
                    .willThrow(new SoldOutException("Tickets are sold out"));

            // Act & Assert
            mockMvc.perform(post("/api/tickets/buy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Sold Out"))
                    .andExpect(jsonPath("$.detail").value("Tickets are sold out"));
        }

        @Test
        @DisplayName("Should return 503 Service Unavailable when database partition / timeout occurs (CP)")
        void shouldReturn503ServiceUnavailableWhenDatabasePartitions() throws Exception {
            // Arrange
            BuyTicketRequest request = new BuyTicketRequest("user-102");
            given(ticketPurchaseService.buyTicket(any(BuyTicketRequest.class)))
                    .willThrow(new ServiceUnavailableException("Database unavailable or lock timeout"));

            // Act & Assert
            mockMvc.perform(post("/api/tickets/buy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.title").value("Service Unavailable"))
                    .andExpect(jsonPath("$.detail").value("Database unavailable or lock timeout"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when buyerId is blank")
        void shouldReturn400BadRequestWhenPayloadIsInvalid() throws Exception {
            // Arrange
            BuyTicketRequest request = new BuyTicketRequest("");

            // Act & Assert
            mockMvc.perform(post("/api/tickets/buy")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/catalog")
    class CatalogEndpointTests {

        @Test
        @DisplayName("Should return 200 OK and catalog vitrine details from Redis")
        void shouldReturn200AndCatalogDetails() throws Exception {
            // Arrange
            CatalogResponse response = new CatalogResponse(45, List.of("user-1", "user-2"), "REDIS", false, 0);
            given(catalogService.getCatalog()).willReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/tickets/catalog")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableStock").value(45))
                    .andExpect(jsonPath("$.dataSource").value("REDIS"))
                    .andExpect(jsonPath("$.driftDetected").value(false))
                    .andExpect(jsonPath("$.recentBuyers[0]").value("user-1"))
                    .andExpect(jsonPath("$.recentBuyers[1]").value("user-2"));
        }
    }
}
