package com.checkout.payment.gateway.bank;

/** Talks to the acquiring bank. An interface so the service can be tested with a fake bank. */
public interface BankClient {

  /**
   * Asks the bank to authorize a payment.
   *
   * @throws com.checkout.payment.gateway.exception.BankUnavailableException if the bank gives no
   *     usable answer
   */
  BankResponse authorize(BankRequest request);
}
