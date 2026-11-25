package com.checkout.payment.gateway.bank;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Request body the bank simulator expects. Field names and the MM/YYYY date are the bank's. */
public record BankRequest(
    @JsonProperty("card_number") String cardNumber,
    @JsonProperty("expiry_date") String expiryDate,
    @JsonProperty("currency") String currency,
    @JsonProperty("amount") long amount,
    @JsonProperty("cvv") String cvv) {

  public static BankRequest from(PostPaymentRequest request) {
    String expiryDate = String.format("%02d/%d", request.expiryMonth(), request.expiryYear());
    return new BankRequest(request.cardNumber(), expiryDate, request.currency(),
        request.amount(), request.cvv());
  }

  // Never print the card number or CVV.
  @Override
  public String toString() {
    return "BankRequest{currency=" + currency + ", amount=" + amount + "}";
  }
}
