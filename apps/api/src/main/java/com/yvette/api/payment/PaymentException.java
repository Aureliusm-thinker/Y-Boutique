package com.yvette.api.payment;

/** A payment couldn't be started or completed; the message is safe to show to the shopper. */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}
