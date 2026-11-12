package com.chaoslab.flashsale.dto;

import java.util.List;

public record CatalogResponse(
        int availableStock,
        List<String> recentBuyers,
        String dataSource,
        boolean driftDetected,
        int driftAmount
) {
}
