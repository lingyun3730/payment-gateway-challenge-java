package com.checkout.payment.gateway.model;

/**
 * Fields are boxed types so that a missing field is null and can be reported by the validator.
 * cardNumber and cvv are Strings to keep leading zeros.
 */
public record PostPaymentRequest(
    String cardNumber,
    Integer expiryMonth,
    Integer expiryYear,
    String currency,
    Long amount,
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
