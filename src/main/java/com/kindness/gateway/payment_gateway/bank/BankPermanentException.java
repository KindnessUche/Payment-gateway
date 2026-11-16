package com.kindness.gateway.payment_gateway.bank;

public class BankPermanentException extends BankException {

    private final int httpStatus;
    private final String errorCode;

    public BankPermanentException(int httpStatus, String errorCode, String message) {
        super(message, null);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getErrorCode() { return errorCode; }
}