package com.bustrack.payment.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CUSTOM, customFaultCode = "{http://bustrack.com/payment}CARD_DECLINED")
public class CardDeclinedException extends RuntimeException {
    public CardDeclinedException(String message) { super(message); }
}
