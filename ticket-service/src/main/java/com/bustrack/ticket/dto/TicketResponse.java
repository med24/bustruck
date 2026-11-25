package com.bustrack.ticket.dto;

import com.bustrack.ticket.model.Ticket;
import com.bustrack.ticket.model.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TicketResponse(Long id, Long lineId, String passengerName, BigDecimal price, TicketStatus status,
                             Long paymentId, Instant createdAt, Instant validatedAt) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getLineId(), t.getPassengerName(), t.getPrice(), t.getStatus(),
                t.getPaymentId(), t.getCreatedAt(), t.getValidatedAt());
    }
}
