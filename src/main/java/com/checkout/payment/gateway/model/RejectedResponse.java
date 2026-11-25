package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.List;

/** Returned with 422 when the request is invalid. No payment is created. */
public record RejectedResponse(PaymentStatus status, List<FieldError> errors) {

  public record FieldError(String field, String message) {
  }

  public static RejectedResponse of(List<FieldError> errors) {
    return new RejectedResponse(PaymentStatus.REJECTED, errors);
  }
}
