package com.bustrack.ticket.service;

import com.bustrack.ticket.client.PaymentClient;
import com.bustrack.ticket.client.PaymentResult;
import com.bustrack.ticket.dto.CreateTicketRequest;
import com.bustrack.ticket.exception.NotFoundException;
import com.bustrack.ticket.exception.PaymentRejectedException;
import com.bustrack.ticket.exception.PaymentUnavailableException;
import com.bustrack.ticket.exception.TicketStateException;
import com.bustrack.ticket.model.Line;
import com.bustrack.ticket.model.Ticket;
import com.bustrack.ticket.model.TicketStatus;
import com.bustrack.ticket.repository.LineRepository;
import com.bustrack.ticket.repository.TicketRepository;
import org.springframework.stereotype.Service;

/**
 * Purchase orchestration: PENDING -> (SOAP AuthorizePayment) -> PAID | CANCELLED, or stays PENDING if payment is down.
 * Deliberately not @Transactional: each state change must be committed independently of the remote call.
 */
@Service
public class TicketService {

    private final TicketRepository tickets;
    private final LineRepository lines;
    private final PaymentClient payments;

    public TicketService(TicketRepository tickets, LineRepository lines, PaymentClient payments) {
        this.tickets = tickets;
        this.lines = lines;
        this.payments = payments;
    }

    public Ticket purchase(CreateTicketRequest request) {
        Line line = lines.findById(request.lineId())
                .orElseThrow(() -> new NotFoundException("Line " + request.lineId() + " not found"));

        Ticket ticket = tickets.save(new Ticket(line.getId(), request.passengerName(), line.getPrice()));
        try {
            PaymentResult result = payments.authorize(ticket.getId(), ticket.getPrice(), request.cardNumber());
            ticket.markPaid(result.paymentId());
            return tickets.save(ticket);
        } catch (PaymentRejectedException e) {
            ticket.markCancelled();
            tickets.save(ticket);
            throw e;
        } catch (PaymentUnavailableException e) {
            e.setTicketId(ticket.getId());      // ticket remains PENDING
            throw e;
        }
    }

    public Ticket get(Long id) {
        return tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
    }

    public Ticket validate(Long id) {
        Ticket ticket = get(id);
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new TicketStateException("Only PAID tickets can be validated (current: " + ticket.getStatus() + ")");
        }
        ticket.markUsed();
        return tickets.save(ticket);
    }
}
