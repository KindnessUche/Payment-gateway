package com.kindness.gateway.payment_gateway.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankRefundRequest(
        @JsonProperty("capture_id") String captureId,
        @JsonProperty("amount") long amountCents) {}