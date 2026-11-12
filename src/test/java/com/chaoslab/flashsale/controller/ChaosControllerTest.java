package com.chaoslab.flashsale.controller;

import com.chaoslab.flashsale.dto.ChaosScenarioType;
import com.chaoslab.flashsale.dto.ChaosStatusResponse;
import com.chaoslab.flashsale.service.ChaosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChaosController Standalone MockMvc Tests")
class ChaosControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChaosService chaosService;

    @InjectMocks
    private ChaosController chaosController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chaosController).build();
    }

    @Test
    @DisplayName("POST /api/chaos/scenario/HEALTHY should return 200 OK and updated scenario status")
    void shouldSwitchToHealthyScenario() throws Exception {
        // Arrange
        ChaosStatusResponse response = new ChaosStatusResponse("HEALTHY", true, true, "Network is 100% operational");
        given(chaosService.applyScenario(ChaosScenarioType.HEALTHY)).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/chaos/scenario/HEALTHY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeScenario").value("HEALTHY"))
                .andExpect(jsonPath("$.postgresHealthy").value(true))
                .andExpect(jsonPath("$.redisHealthy").value(true));
    }

    @Test
    @DisplayName("POST /api/chaos/scenario/PARTITION_CP should return 200 OK and disabled postgres status")
    void shouldSwitchToPartitionCpScenario() throws Exception {
        // Arrange
        ChaosStatusResponse response = new ChaosStatusResponse("PARTITION_CP", false, true, "PostgreSQL partitioned");
        given(chaosService.applyScenario(ChaosScenarioType.PARTITION_CP)).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/chaos/scenario/PARTITION_CP")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeScenario").value("PARTITION_CP"))
                .andExpect(jsonPath("$.postgresHealthy").value(false))
                .andExpect(jsonPath("$.redisHealthy").value(true));
    }

    @Test
    @DisplayName("GET /api/chaos/status should return 200 OK and current network status")
    void shouldReturnCurrentChaosStatus() throws Exception {
        // Arrange
        ChaosStatusResponse response = new ChaosStatusResponse("HEALTHY", true, true, "All proxies active");
        given(chaosService.getStatus()).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/chaos/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeScenario").value("HEALTHY"))
                .andExpect(jsonPath("$.postgresHealthy").value(true))
                .andExpect(jsonPath("$.redisHealthy").value(true));
    }
}
