package com.kindness.gateway.payment_gateway.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankCaptureRequest(
        @JsonProperty("authorization_id") String authorizationId,
        @JsonProperty("amount") long amountCents) {}