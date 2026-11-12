package com.chaoslab.flashsale.controller;

import com.chaoslab.flashsale.dto.TelemetryResponse;
import com.chaoslab.flashsale.service.TelemetryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public TelemetryResponse getTelemetry() {
        return telemetryService.getTelemetry();
    }
}
