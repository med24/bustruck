package com.bustrack.ticket.controller;

import com.bustrack.ticket.dto.CreateTicketRequest;
import com.bustrack.ticket.dto.TicketResponse;
import com.bustrack.ticket.model.Ticket;
import com.bustrack.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = service.purchase(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(ticket.getId()).toUri();
        return ResponseEntity.created(location).body(TicketResponse.from(ticket));
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(service.get(id));
    }

    @PostMapping("/{id}/validate")
    public TicketResponse validate(@PathVariable Long id) {
        return TicketResponse.from(service.validate(id));
    }
}
