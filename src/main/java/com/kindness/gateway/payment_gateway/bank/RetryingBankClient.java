package com.kindness.gateway.payment_gateway.bank;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RetryingBankClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 200;

    private final BankClient bankClient;

    public RetryingBankClient(BankClient bankClient) {
        this.bankClient = bankClient;
    }

    public BankAuthorizeResponse authorize(BankAuthorizeRequest request, String idempotencyKey) {
        BankTransientException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return bankClient.authorize(request, idempotencyKey);
            } catch (BankTransientException e) {
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleep(delayFor(attempt));
                }
            }
        }
        throw lastFailure;
    }
    public BankCaptureResponse capture(BankCaptureRequest request,
                                 String idempotencyKey) {
        BankTransientException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return bankClient.capture(request, idempotencyKey);
            } catch (BankTransientException e) {
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleep(delayFor(attempt));
                }
            }
        }
        throw lastFailure;
    }


    private long delayFor(int attempt) {
        long maxRange = BASE_DELAY_MS * (1L << (attempt - 1));
        return ThreadLocalRandom.current().nextLong(maxRange);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt(); // restore the flag, then stop waiting
        }
    }
}