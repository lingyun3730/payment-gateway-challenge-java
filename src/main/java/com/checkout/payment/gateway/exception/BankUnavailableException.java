package com.checkout.payment.gateway.exception;

/** The acquiring bank could not give us an answer (down, timed out, or sent an unusable reply). */
public class BankUnavailableException extends RuntimeException {
  public BankUnavailableException(String message) {
    super(message);
  }

  public BankUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
