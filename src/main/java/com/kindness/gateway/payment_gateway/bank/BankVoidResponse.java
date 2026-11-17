package com.kindness.gateway.payment_gateway.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;


@JsonIgnoreProperties(ignoreUnknown = true)
public record BankVoidResponse(
        @JsonProperty("void_id") String voidId,
        @JsonProperty("authorization_id") String authorizationId,
        @JsonProperty("voided_at") Instant voidedAt) {}
