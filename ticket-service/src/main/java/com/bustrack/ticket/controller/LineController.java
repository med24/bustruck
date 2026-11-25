package com.bustrack.ticket.controller;

import com.bustrack.ticket.dto.LineDto;
import com.bustrack.ticket.dto.LineRequest;
import com.bustrack.ticket.dto.NextBusResponse;
import com.bustrack.ticket.model.Line;
import com.bustrack.ticket.service.LineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lines")
public class LineController {

    private final LineService service;

    public LineController(LineService service) {
        this.service = service;
    }

    @GetMapping
    public List<LineDto> list() {
        return service.findAll().stream().map(LineDto::from).toList();
    }

    @GetMapping("/{id}")
    public LineDto get(@PathVariable Long id) {
        return LineDto.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<LineDto> create(@Valid @RequestBody LineRequest request) {
        Line line = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(line.getId()).toUri();
        return ResponseEntity.created(location).body(LineDto.from(line));
    }

    @GetMapping("/{id}/next-bus")
    public NextBusResponse nextBus(@PathVariable Long id) {
        return service.nextBus(id);
    }
}
