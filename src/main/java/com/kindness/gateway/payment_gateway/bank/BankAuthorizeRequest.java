package com.kindness.gateway.payment_gateway.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankAuthorizeRequest(
        @JsonProperty("card_number") String cardNumber,
        @JsonProperty("cvv") String cvv,
        @JsonProperty("expiry_month") int expiryMonth,
        @JsonProperty("expiry_year") int expiryYear,
        @JsonProperty("amount") long amountCents) {
}