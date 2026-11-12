package com.chaoslab.flashsale.controller;

import com.chaoslab.flashsale.dto.ResetResponse;
import com.chaoslab.flashsale.service.AdminResetService;
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

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Standalone MockMvc Tests")
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminResetService adminResetService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    @DisplayName("POST /api/admin/reset should return 200 OK and reset details")
    void shouldResetEnvironmentSuccessfully() throws Exception {
        // Arrange
        ResetResponse response = new ResetResponse(50, "Environment reset successfully to 50 tickets and HEALTHY topology", Instant.now());
        given(adminResetService.resetEnvironment()).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/admin/reset")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockResetTo").value(50))
                .andExpect(jsonPath("$.message").value("Environment reset successfully to 50 tickets and HEALTHY topology"));
    }
}
