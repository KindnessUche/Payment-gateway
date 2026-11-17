package com.kindness.gateway.payment_gateway.bank;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankCaptureResponse(
        @JsonProperty("capture_id") String captureId,
        @JsonProperty("authorization_id") String authorizationId,
        @JsonProperty("amount") long amountCents,
        @JsonProperty("captured_at") Instant capturedAt) {}