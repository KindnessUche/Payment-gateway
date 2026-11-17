package com.kindness.gateway.payment_gateway.bank;

import org.springframework.stereotype.Component;
import java.util.function.Supplier;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RetryingBankClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 200;

    private final BankClient bankClient;

    public RetryingBankClient(BankClient bankClient) {
        this.bankClient = bankClient;
    }


    private <Res> Res retry(Supplier<Res> call) {
        BankTransientException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (BankTransientException e) {
                lastFailure = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleep(delayFor(attempt));
                }
            }
        }
        throw lastFailure;
    }

    public BankAuthorizeResponse authorize(BankAuthorizeRequest request, String key) {
        return retry(() -> bankClient.authorize(request, key));
    }

    public BankCaptureResponse capture(BankCaptureRequest request, String key) {
        return retry(() -> bankClient.capture(request, key));
    }

    public BankVoidResponse voidAuthorization(BankVoidRequest request, String key) {
        return retry(() -> bankClient.voidAuthorization(request, key));
    }

    public BankRefundResponse refund(BankRefundRequest request, String key) {
        return retry(() -> bankClient.refund(request, key));
    }



    private long delayFor(int attempt) {
        long maxRange = BASE_DELAY_MS * (1L << (attempt - 1));
        return ThreadLocalRandom.current().nextLong(maxRange);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}