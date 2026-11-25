package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.RejectedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleException(EventProcessingException ex) {
    LOG.error("Exception happened", ex);
    return new ResponseEntity<>(new ErrorResponse("Page not found"),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(PaymentRejectedException.class)
  public ResponseEntity<RejectedResponse> handleRejected(PaymentRejectedException ex) {
    return new ResponseEntity<>(RejectedResponse.of(ex.getErrors()),
        HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @ExceptionHandler(BankUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleBankUnavailable(BankUnavailableException ex) {
    LOG.error("Acquiring bank unavailable: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse("Acquiring bank is unavailable"),
        HttpStatus.BAD_GATEWAY);
  }

  // The message is fixed on purpose: Jackson's own message can quote the submitted value,
  // which may be a card number.
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
    return new ResponseEntity<>(new ErrorResponse("Request body is missing or malformed"),
        HttpStatus.BAD_REQUEST);
  }
}
