package com.chaoslab.flashsale.controller;

import com.chaoslab.flashsale.dto.BuyTicketRequest;
import com.chaoslab.flashsale.dto.BuyTicketResponse;
import com.chaoslab.flashsale.dto.CatalogResponse;
import com.chaoslab.flashsale.service.CatalogService;
import com.chaoslab.flashsale.service.TicketPurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketPurchaseService ticketPurchaseService;
    private final CatalogService catalogService;

    public TicketController(TicketPurchaseService ticketPurchaseService, CatalogService catalogService) {
        this.ticketPurchaseService = ticketPurchaseService;
        this.catalogService = catalogService;
    }

    @PostMapping("/buy")
    @ResponseStatus(HttpStatus.CREATED)
    public BuyTicketResponse buyTicket(@Valid @RequestBody BuyTicketRequest request) {
        return ticketPurchaseService.buyTicket(request);
    }

    @GetMapping("/catalog")
    public CatalogResponse getCatalog() {
        return catalogService.getCatalog();
    }
}
