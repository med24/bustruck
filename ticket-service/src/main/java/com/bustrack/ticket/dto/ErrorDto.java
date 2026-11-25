package com.bustrack.ticket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDto(String code, String message, Instant timestamp, Long ticketId, String ticketStatus) {
    public static ErrorDto of(String code, String message) {
        return new ErrorDto(code, message, Instant.now(), null, null);
    }
}
