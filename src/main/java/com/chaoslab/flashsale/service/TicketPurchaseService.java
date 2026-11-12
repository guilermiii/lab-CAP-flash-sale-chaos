package com.chaoslab.flashsale.service;

import com.chaoslab.flashsale.domain.TicketInventory;
import com.chaoslab.flashsale.domain.TicketOrder;
import com.chaoslab.flashsale.dto.BuyTicketRequest;
import com.chaoslab.flashsale.dto.BuyTicketResponse;
import com.chaoslab.flashsale.exception.ServiceUnavailableException;
import com.chaoslab.flashsale.exception.SoldOutException;
import com.chaoslab.flashsale.repository.TicketInventoryRepository;
import com.chaoslab.flashsale.repository.TicketOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TicketPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(TicketPurchaseService.class);

    private final TicketInventoryRepository inventoryRepository;
    private final TicketOrderRepository orderRepository;
    private final CatalogService catalogService;

    private static final Long DEFAULT_INVENTORY_ID = 1L;

    public TicketPurchaseService(
            TicketInventoryRepository inventoryRepository,
            TicketOrderRepository orderRepository,
            CatalogService catalogService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.catalogService = catalogService;
    }

    @Transactional
    public BuyTicketResponse buyTicket(BuyTicketRequest request) {
        TicketInventory inventory;
        try {
            inventory = inventoryRepository.findWithLockById(DEFAULT_INVENTORY_ID)
                    .orElseThrow(() -> new ServiceUnavailableException("Inventory not found or uninitialized"));
        } catch (DataAccessException ex) {
            log.error("Failed to acquire lock or query inventory from database: {}", ex.getMessage());
            throw new ServiceUnavailableException("Database unavailable or lock timeout", ex);
        }

        if (!inventory.hasAvailableStock()) {
            log.warn("Ticket purchase rejected: stock is 0 for buyer {}", request.buyerId());
            throw new SoldOutException("Tickets are sold out");
        }

        inventory.decrementStock();
        inventoryRepository.save(inventory);

        TicketOrder order = TicketOrder.builder()
                .buyerId(request.buyerId())
                .purchasedAt(Instant.now())
                .status("CONFIRMED")
                .build();
        TicketOrder savedOrder = orderRepository.save(order);

        // Dispara propagação assíncrona eventual para o Redis (BASE)
        catalogService.syncCatalogAsync(inventory.getAvailableQuantity(), request.buyerId());

        return new BuyTicketResponse(
                savedOrder.getId(),
                savedOrder.getBuyerId(),
                savedOrder.getPurchasedAt(),
                inventory.getAvailableQuantity()
        );
    }
}
