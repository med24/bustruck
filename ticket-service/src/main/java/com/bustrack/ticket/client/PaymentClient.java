package com.bustrack.ticket.client;

import com.bustrack.ticket.exception.PaymentRejectedException;
import com.bustrack.ticket.exception.PaymentUnavailableException;
import com.bustrack.ticket.soap.AuthorizePaymentRequest;
import com.bustrack.ticket.soap.AuthorizePaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.math.BigDecimal;

/**
 * SOAP client of payment-service.
 * Resilience: Retry (exponential backoff, technical errors only) wraps a CircuitBreaker; when everything fails the
 * Retry fallback throws PaymentUnavailableException -> the ticket stays PENDING and the API answers 503.
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final WebServiceTemplate template;

    public PaymentClient(WebServiceTemplate paymentWebServiceTemplate) {
        this.template = paymentWebServiceTemplate;
    }

    @Retry(name = "payment", fallbackMethod = "authorizeFallback")
    @CircuitBreaker(name = "payment")
    public PaymentResult authorize(Long ticketId, BigDecimal amount, String cardNumber) {
        AuthorizePaymentRequest request = new AuthorizePaymentRequest();
        request.setTicketId(ticketId);
        request.setAmount(amount);
        request.setCardNumber(cardNumber);
        try {
            AuthorizePaymentResponse response = (AuthorizePaymentResponse) template.marshalSendAndReceive(request);
            return new PaymentResult(response.getPaymentId(), response.getStatus());
        } catch (SoapFaultClientException fault) {
            // Business refusal (SOAP Fault): not retried, not counted by the circuit breaker
            String code = fault.getFaultCode() != null ? fault.getFaultCode().getLocalPart() : "PAYMENT_REFUSED";
            throw new PaymentRejectedException(code, fault.getFaultStringOrReason());
        }
    }

    public PaymentResult authorizeFallback(Long ticketId, BigDecimal amount, String cardNumber, Throwable t) {
        if (t instanceof PaymentRejectedException rejected) {
            throw rejected;
        }
        log.warn("payment-service unavailable for ticket {}: {}", ticketId, t.toString());
        throw new PaymentUnavailableException("Payment service unavailable, ticket kept PENDING");
    }
}
