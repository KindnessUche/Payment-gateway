package com.kindness.gateway.payment_gateway.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankVoidRequest(
        @JsonProperty("authorization_id") String authorizationId) {}
