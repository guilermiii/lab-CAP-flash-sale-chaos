package com.chaoslab.flashsale.controller;

import com.chaoslab.flashsale.dto.ChaosScenarioType;
import com.chaoslab.flashsale.dto.ChaosStatusResponse;
import com.chaoslab.flashsale.service.ChaosService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chaos")
public class ChaosController {

    private final ChaosService chaosService;

    public ChaosController(ChaosService chaosService) {
        this.chaosService = chaosService;
    }

    @PostMapping("/scenario/{tipo}")
    public ChaosStatusResponse applyScenario(@PathVariable("tipo") ChaosScenarioType tipo) {
        return chaosService.applyScenario(tipo);
    }

    @GetMapping("/status")
    public ChaosStatusResponse getStatus() {
        return chaosService.getStatus();
    }
}
