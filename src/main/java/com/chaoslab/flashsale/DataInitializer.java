package com.chaoslab.flashsale;

import com.chaoslab.flashsale.domain.TicketInventory;
import com.chaoslab.flashsale.repository.TicketInventoryRepository;
import com.chaoslab.flashsale.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TicketInventoryRepository inventoryRepository;
    private final CatalogService catalogService;

    private static final Long DEFAULT_INVENTORY_ID = 1L;
    private static final int INITIAL_STOCK = 50;

    public DataInitializer(TicketInventoryRepository inventoryRepository, CatalogService catalogService) {
        this.inventoryRepository = inventoryRepository;
        this.catalogService = catalogService;
    }

    @Override
    public void run(String... args) {
        try {
            if (!inventoryRepository.existsById(DEFAULT_INVENTORY_ID)) {
                log.info("Initializing inventory with {} tickets...", INITIAL_STOCK);
                TicketInventory inventory = TicketInventory.builder()
                        .id(DEFAULT_INVENTORY_ID)
                        .totalQuantity(INITIAL_STOCK)
                        .availableQuantity(INITIAL_STOCK)
                        .build();
                inventoryRepository.save(inventory);
                catalogService.resetCatalog(INITIAL_STOCK);
                log.info("Database and Redis catalog initialized with {} tickets.", INITIAL_STOCK);
            }
        } catch (Exception ex) {
            log.warn("Database/Redis initialization skipped or failed during startup: {}", ex.getMessage());
        }
    }
}
