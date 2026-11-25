package com.bustrack.ticket.exception;

/** Technical failure: payment-service unreachable / circuit open. The ticket stays PENDING. */
public class PaymentUnavailableException extends RuntimeException {
    private Long ticketId;

    public PaymentUnavailableException(String message) { super(message); }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
}
