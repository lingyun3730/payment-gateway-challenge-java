package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.RejectedResponse.FieldError;
import java.util.List;

/** The request failed validation, so no payment was created and the bank was not called. */
public class PaymentRejectedException extends RuntimeException {

  private final List<FieldError> errors;

  public PaymentRejectedException(List<FieldError> errors) {
    super("Payment request rejected");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> getErrors() {
    return errors;
  }
}
