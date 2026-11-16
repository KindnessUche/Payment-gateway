package com.kindness.gateway.payment_gateway.bank;

public class BankTransientException extends BankException {

    public BankTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}