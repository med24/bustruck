package com.bustrack.ticket.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long lineId;
    private String passengerName;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private TicketStatus status = TicketStatus.PENDING;

    private Long paymentId;
    private Instant createdAt = Instant.now();
    private Instant validatedAt;

    protected Ticket() {}

    public Ticket(Long lineId, String passengerName, BigDecimal price) {
        this.lineId = lineId;
        this.passengerName = passengerName;
        this.price = price;
    }

    public void markPaid(Long paymentId) { this.paymentId = paymentId; this.status = TicketStatus.PAID; }
    public void markCancelled() { this.status = TicketStatus.CANCELLED; }
    public void markUsed() { this.status = TicketStatus.USED; this.validatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getLineId() { return lineId; }
    public String getPassengerName() { return passengerName; }
    public BigDecimal getPrice() { return price; }
    public TicketStatus getStatus() { return status; }
    public Long getPaymentId() { return paymentId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getValidatedAt() { return validatedAt; }
}
