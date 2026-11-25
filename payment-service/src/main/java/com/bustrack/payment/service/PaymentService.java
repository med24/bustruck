package com.bustrack.payment.service;

import com.bustrack.payment.domain.Payment;
import com.bustrack.payment.domain.PaymentRepository;
import com.bustrack.payment.domain.PaymentStatus;
import com.bustrack.payment.exception.CardDeclinedException;
import com.bustrack.payment.exception.InvalidCardException;
import com.bustrack.payment.exception.PaymentNotFoundException;
import com.bustrack.payment.exception.PaymentStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Simulated bank rules:
 *  - card must be 13-19 digits, otherwise INVALID_CARD
 *  - card ending in 0002  -> CARD_DECLINED (insufficient funds)
 *  - amount above 1000    -> CARD_DECLINED (limit exceeded)
 *  - anything else        -> APPROVED
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal LIMIT = new BigDecimal("1000");

    private final PaymentRepository repository;

    public PaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    // Not @Transactional on purpose: a DECLINED payment must stay persisted even though a fault is thrown.
    public Payment authorize(long ticketId, BigDecimal amount, String cardNumber) {
        if (cardNumber == null || !cardNumber.matches("\\d{13,19}")) {
            throw new InvalidCardException("Invalid card number");
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);

        if (cardNumber.endsWith("0002")) {
            repository.save(new Payment(ticketId, amount, last4, PaymentStatus.DECLINED));
            log.info("Payment declined for ticket {} (insufficient funds)", ticketId);
            throw new CardDeclinedException("Insufficient funds");
        }
        if (amount.compareTo(LIMIT) > 0) {
            repository.save(new Payment(ticketId, amount, last4, PaymentStatus.DECLINED));
            throw new CardDeclinedException("Limit exceeded");
        }
        Payment payment = repository.save(new Payment(ticketId, amount, last4, PaymentStatus.APPROVED));
        log.info("Payment {} approved for ticket {}", payment.getId(), ticketId);
        return payment;
    }

    public Payment refund(long paymentId) {
        Payment payment = get(paymentId);
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            throw new PaymentStateException("Only APPROVED payments can be refunded (current: " + payment.getStatus() + ")");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        return repository.save(payment);
    }

    public Payment get(long paymentId) {
        return repository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment " + paymentId + " not found"));
    }
}
