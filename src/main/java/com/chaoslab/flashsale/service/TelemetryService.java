package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.dto.TelemetryRecord;
import com.chaoslab.flashsale.dto.TelemetryResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TelemetryService {

    private final AtomicInteger count201 = new AtomicInteger(0);
    private final AtomicInteger count409 = new AtomicInteger(0);
    private final AtomicInteger count503 = new AtomicInteger(0);
    private final AtomicInteger countOthers = new AtomicInteger(0);

    private final ConcurrentLinkedDeque<TelemetryRecord> recentTransactions = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Long> requestTimestamps = new ConcurrentLinkedDeque<>();

    private static final int MAX_RECENT_RECORDS = 15;
    private static final long WINDOW_MS = 5000L;

    public void recordTransaction(String endpoint, String method, int statusCode, long durationMs) {
        long now = System.currentTimeMillis();
        requestTimestamps.addLast(now);

        // Atualiza contadores de status
        switch (statusCode) {
            case 201 -> count201.incrementAndGet();
            case 409 -> count409.incrementAndGet();
            case 503 -> count503.incrementAndGet();
            default -> countOthers.incrementAndGet();
        }

        // Adiciona à lista de transações recentes
        String timestampStr = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        TelemetryRecord record = new TelemetryRecord(
                UUID.randomUUID().toString().substring(0, 8),
                timestampStr,
                endpoint,
                method,
                statusCode,
                durationMs
        );

        recentTransactions.addFirst(record);
        while (recentTransactions.size() > MAX_RECENT_RECORDS) {
            recentTransactions.pollLast();
        }

        // Limpa timestamps antigos do sliding window
        cleanOldTimestamps(now);
    }

    public TelemetryResponse getTelemetry() {
        long now = System.currentTimeMillis();
        cleanOldTimestamps(now);

        double rps = requestTimestamps.size() / (WINDOW_MS / 1000.0);
        // Formata para 1 casa decimal
        rps = Math.round(rps * 10.0) / 10.0;

        return new TelemetryResponse(
                rps,
                count201.get(),
                count409.get(),
                count503.get(),
                countOthers.get(),
                new ArrayList<>(recentTransactions)
        );
    }

    public void reset() {
        count201.set(0);
        count409.set(0);
        count503.set(0);
        countOthers.set(0);
        recentTransactions.clear();
        requestTimestamps.clear();
    }

    private void cleanOldTimestamps(long now) {
        long cutoff = now - WINDOW_MS;
        while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() < cutoff) {
            requestTimestamps.pollFirst();
        }
    }
}
