package com.kindness.gateway.payment_gateway.bank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankErrorResponse(String error, String message) {
}