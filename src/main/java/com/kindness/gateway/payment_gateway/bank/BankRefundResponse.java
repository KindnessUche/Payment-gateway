package com.kindness.gateway.payment_gateway.bank;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankRefundResponse(
        @JsonProperty("refund_id") String refundId,
        @JsonProperty("capture_id") String captureId,
        @JsonProperty("amount") long amountCents,
        @JsonProperty("refunded_at") Instant refundedAt) {}