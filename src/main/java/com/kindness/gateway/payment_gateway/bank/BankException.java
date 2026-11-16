package com.kindness.gateway.payment_gateway.bank;

public abstract class BankException extends RuntimeException {

    protected BankException(String message, Throwable cause) {
        super(message, cause);
    }
}