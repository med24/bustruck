package com.bustrack.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    /** Only the last 4 digits are kept; the full card number is never stored. */
    private String cardLast4;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant createdAt = Instant.now();

    protected Payment() {}

    public Payment(Long ticketId, BigDecimal amount, String cardLast4, PaymentStatus status) {
        this.ticketId = ticketId;
        this.amount = amount;
        this.cardLast4 = cardLast4;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public BigDecimal getAmount() { return amount; }
    public String getCardLast4() { return cardLast4; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
