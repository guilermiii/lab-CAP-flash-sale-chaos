package com.chaoslab.flashsale.controller;

import com.chaoslab.flashsale.dto.TelemetryResponse;
import com.chaoslab.flashsale.service.TelemetryService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryController Standalone MockMvc Tests")
class TelemetryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TelemetryService telemetryService;

    @InjectMocks
    private TelemetryController telemetryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(telemetryController).build();
    }

    @Test
    @DisplayName("GET /api/telemetry should return 200 OK and metrics")
    void shouldReturnTelemetryData() throws Exception {
        // Arrange
        TelemetryResponse response = new TelemetryResponse(12.5, 50, 20, 5, 0, List.of());
        given(telemetryService.getTelemetry()).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/telemetry")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rps").value(12.5))
                .andExpect(jsonPath("$.total201").value(50))
                .andExpect(jsonPath("$.total409").value(20))
                .andExpect(jsonPath("$.total503").value(5));
    }
}
