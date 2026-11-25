package com.checkout.payment.gateway.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankResponse(
    @JsonProperty("authorized") boolean authorized,
    @JsonProperty("authorization_code") String authorizationCode) {
}
