package com.bustrack.ticket.exception;

/** Business refusal coming from a SOAP Fault (card declined, invalid card...). */
public class PaymentRejectedException extends RuntimeException {
    private final String code;

    public PaymentRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
