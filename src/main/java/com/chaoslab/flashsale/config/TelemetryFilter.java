package com.chaoslab.flashsale.config;

import com.chaoslab.flashsale.service.TelemetryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class TelemetryFilter extends OncePerRequestFilter {

    private final TelemetryService telemetryService;

    public TelemetryFilter(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // Não registra chamadas ao próprio endpoint de telemetria
        if (uri.startsWith("/api/telemetry") || !uri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            telemetryService.recordTransaction(uri, request.getMethod(), response.getStatus(), duration);
        }
    }
}
