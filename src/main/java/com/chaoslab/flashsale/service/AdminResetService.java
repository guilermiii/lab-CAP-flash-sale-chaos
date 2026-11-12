package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.domain.TicketInventory;
import com.chaoslab.flashsale.dto.ChaosScenarioType;
import com.chaoslab.flashsale.dto.ResetResponse;
import com.chaoslab.flashsale.repository.TicketInventoryRepository;
import com.chaoslab.flashsale.repository.TicketOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AdminResetService {

    private static final Logger log = LoggerFactory.getLogger(AdminResetService.class);

    private final TicketInventoryRepository inventoryRepository;
    private final TicketOrderRepository orderRepository;
    private final CatalogService catalogService;
    private final ChaosService chaosService;

    private static final Long DEFAULT_INVENTORY_ID = 1L;
    private static final int DEFAULT_STOCK = 50;

    public AdminResetService(
            TicketInventoryRepository inventoryRepository,
            TicketOrderRepository orderRepository,
            CatalogService catalogService,
            ChaosService chaosService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
        this.chaosService = chaosService;
    }

    @Transactional
    public ResetResponse resetEnvironment() {
        log.info("Resetting environment to {} tickets...", DEFAULT_STOCK);

        // 1. Limpa todas as ordens anteriores
        orderRepository.deleteAll();

        // 2. Restaura o saldo no PostgreSQL para 50
        TicketInventory inventory = inventoryRepository.findById(DEFAULT_INVENTORY_ID)
                .orElseGet(() -> TicketInventory.builder().id(DEFAULT_INVENTORY_ID).build());

        inventory.setTotalQuantity(DEFAULT_STOCK);
        inventory.setAvailableQuantity(DEFAULT_STOCK);
        inventoryRepository.save(inventory);

        // 3. Sincroniza o Redis catalog com 50 ingressos e limpa últimos compradores
        catalogService.resetCatalog(DEFAULT_STOCK);

        // 4. Remove qualquer toxic e restaura o Toxiproxy para HEALTHY
        chaosService.applyScenario(ChaosScenarioType.HEALTHY);

        log.info("Environment successfully reset to {} tickets and HEALTHY topology", DEFAULT_STOCK);

        return new ResetResponse(
                DEFAULT_STOCK,
                "Environment reset successfully to 50 tickets and HEALTHY topology",
                Instant.now()
        );
    }
}
