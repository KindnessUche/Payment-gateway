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

    private <Req, Res> Res post(String path, Req request, String idempotencyKey, Class<Res> responseType) {
        try {
            return restClient.post()
                    .uri(path)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .body(responseType);

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

    public BankAuthorizeResponse authorize(BankAuthorizeRequest request,
                                   String idempotencyKey) {
        return post("/api/v1/authorizations", request, idempotencyKey,
                BankAuthorizeResponse.class);
    }
    public BankCaptureResponse capture(BankCaptureRequest request, String idempotencyKey) {
        return post("/api/v1/captures", request, idempotencyKey,
                BankCaptureResponse.class);
    }
    public BankVoidResponse voidAuthorization(BankVoidRequest request,
                                  String idempotencyKey) {
        return post("/api/v1/voids", request, idempotencyKey,
                BankVoidResponse.class);
    }
    public BankRefundResponse refund(BankRefundRequest request, String idempotencyKey) {
        return post("/api/v1/refunds", request, idempotencyKey,
                BankRefundResponse.class);
    }
}