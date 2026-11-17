package com.kindness.gateway.payment_gateway.bank;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class BankClient {

    private final RestClient restClient;

    public BankClient(RestClient bankRestClient) {
        this.restClient = bankRestClient;
    }

    public BankAuthorizeResponse authorize(BankAuthorizeRequest request, String idempotencyKey) {
        try {
            return restClient.post()
                    .uri("/api/v1/authorizations")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(BankAuthorizeResponse.class);

        } catch (HttpClientErrorException e) {

            String code = "unknown_error";
            String message = e.getMessage();
            try {
                BankErrorResponse err = e.getResponseBodyAs(BankErrorResponse.class);
                if (err != null && err.error() != null) {
                    code = err.error();
                    if (err.message() != null) {
                        message = err.message();
                    }
                }
            } catch (Exception ignored) {
            }
            throw new BankPermanentException(e.getStatusCode().value(), code, message);

        } catch (HttpServerErrorException e) {
            throw new BankTransientException("Bank returned " + e.getStatusCode().value(), e);

        } catch (ResourceAccessException e) {
            throw new BankTransientException("No response from bank (timeout or connection error)", e);
        }
    }
    public BankCaptureResponse capture(BankCaptureRequest request,
                              String idempotencyKey) {
        try {
            return restClient.post()
                    .uri("/api/v1/captures")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(BankCaptureResponse.class);

        } catch (HttpClientErrorException e) {

            String code = "unknown_error";
            String message = e.getMessage();
            try {
                BankErrorResponse err = e.getResponseBodyAs(BankErrorResponse.class);
                if (err != null && err.error() != null) {
                    code = err.error();
                    if (err.message() != null) {
                        message = err.message();
                    }
                }
            } catch (Exception ignored) {
            }
            throw new BankPermanentException(e.getStatusCode().value(), code, message);

        } catch (HttpServerErrorException e) {
            throw new BankTransientException("Bank returned " + e.getStatusCode().value(), e);

        } catch (ResourceAccessException e) {
            throw new BankTransientException("No response from bank (timeout or connection error)", e);
        }
    }
}