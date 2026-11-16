package com.kindness.gateway.payment_gateway.domain;

public class InvalidTransitionException extends RuntimeException {

    private final PaymentState from;
    private final PaymentState to;

    public InvalidTransitionException(PaymentState from, PaymentState to) {
        super("Cannot move payment from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public PaymentState getFrom() { return from; }
    public PaymentState getTo() { return to; }
}