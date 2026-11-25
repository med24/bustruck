package com.bustrack.payment.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CUSTOM, customFaultCode = "{http://bustrack.com/payment}INVALID_PAYMENT_STATE")
public class PaymentStateException extends RuntimeException {
    public PaymentStateException(String message) { super(message); }
}
