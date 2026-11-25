package com.checkout.payment.gateway.model;

import java.math.BigDecimal;

/**
 * Fields are boxed types so that a missing field is null and can be reported by the validator.
 * cardNumber and cvv are Strings to keep leading zeros. amount is a BigDecimal so that 10.5 is
 * read as a number and rejected by the validator (422) instead of failing JSON parsing (400).
 */
public record PostPaymentRequest(
    String cardNumber,
    Integer expiryMonth,
    Integer expiryYear,
    String currency,
    BigDecimal amount,
    String cvv) {

  // Never print the card number or CVV.
  @Override
  public String toString() {
    return "PostPaymentRequest{expiryMonth=" + expiryMonth
        + ", expiryYear=" + expiryYear
        + ", currency=" + currency
        + ", amount=" + amount + "}";
  }
}
