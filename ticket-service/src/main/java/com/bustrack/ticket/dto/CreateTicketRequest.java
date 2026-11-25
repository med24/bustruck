package com.bustrack.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTicketRequest(
        @NotNull Long lineId,
        @NotBlank String passengerName,
        @NotBlank String cardNumber) {}
