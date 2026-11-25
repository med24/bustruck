package com.bustrack.payment.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CUSTOM, customFaultCode = "{http://bustrack.com/payment}INVALID_CARD")
public class InvalidCardException extends RuntimeException {
    public InvalidCardException(String message) { super(message); }
}
