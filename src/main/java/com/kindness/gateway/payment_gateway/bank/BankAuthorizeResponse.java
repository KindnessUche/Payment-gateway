package com.kindness.gateway.payment_gateway.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankAuthorizeResponse(
        @JsonProperty("authorization_id") String authorizationId,
        @JsonProperty("expires_at") Instant expiresAt) {
}