package com.bustrack.payment.service;

import com.bustrack.payment.config.WebServiceConfig;
import com.bustrack.payment.domain.Payment;
import com.bustrack.payment.ws.*;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/** SOAP operations: AuthorizePayment, RefundPayment, GetPaymentStatus (JAXB classes generated from payment.xsd). */
@Endpoint
public class PaymentEndpoint {

    private static final String NS = WebServiceConfig.NAMESPACE;

    private final PaymentService service;

    public PaymentEndpoint(PaymentService service) {
        this.service = service;
    }

    @PayloadRoot(namespace = NS, localPart = "AuthorizePaymentRequest")
    @ResponsePayload
    public AuthorizePaymentResponse authorize(@RequestPayload AuthorizePaymentRequest request) {
        Payment p = service.authorize(request.getTicketId(), request.getAmount(), request.getCardNumber());
        AuthorizePaymentResponse response = new AuthorizePaymentResponse();
        response.setPaymentId(p.getId());
        response.setStatus(p.getStatus().name());
        return response;
    }

    @PayloadRoot(namespace = NS, localPart = "RefundPaymentRequest")
    @ResponsePayload
    public RefundPaymentResponse refund(@RequestPayload RefundPaymentRequest request) {
        Payment p = service.refund(request.getPaymentId());
        RefundPaymentResponse response = new RefundPaymentResponse();
        response.setPaymentId(p.getId());
        response.setStatus(p.getStatus().name());
        return response;
    }

    @PayloadRoot(namespace = NS, localPart = "GetPaymentStatusRequest")
    @ResponsePayload
    public GetPaymentStatusResponse status(@RequestPayload GetPaymentStatusRequest request) {
        Payment p = service.get(request.getPaymentId());
        GetPaymentStatusResponse response = new GetPaymentStatusResponse();
        response.setPaymentId(p.getId());
        response.setTicketId(p.getTicketId());
        response.setAmount(p.getAmount());
        response.setStatus(p.getStatus().name());
        return response;
    }
}
