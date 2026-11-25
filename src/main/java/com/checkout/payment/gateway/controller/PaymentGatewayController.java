package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @Operation(summary = "Process a card payment",
      description = "Pick an example from the drop-down under Request body. "
          + "Card number ending in an odd digit is authorized, an even digit is declined, "
          + "and 0 makes the bank simulator return 503.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
      mediaType = "application/json",
      examples = {
          @ExampleObject(name = "1. Authorized (200)", value = PaymentExamples.AUTHORIZED),
          @ExampleObject(name = "2. Declined (200)", value = PaymentExamples.DECLINED),
          @ExampleObject(name = "3. Bank unavailable (502)",
              value = PaymentExamples.BANK_UNAVAILABLE),
          @ExampleObject(name = "4. Rejected: several invalid fields (422)",
              value = PaymentExamples.SEVERAL_INVALID_FIELDS),
          @ExampleObject(name = "5. Rejected: fields missing (422)",
              value = PaymentExamples.MISSING_FIELDS),
          @ExampleObject(name = "6. Rejected: card expired (422)",
              value = PaymentExamples.EXPIRED_CARD),
          @ExampleObject(name = "7. Malformed JSON (400)",
              value = PaymentExamples.MALFORMED_JSON),
          @ExampleObject(name = "8. Amount is not an integer (400)",
              value = PaymentExamples.AMOUNT_NOT_AN_INTEGER)
      }))
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "Bank answered: status is Authorized or Declined"),
      @ApiResponse(responseCode = "400", description = "Malformed JSON or wrong field type",
          content = @Content),
      @ApiResponse(responseCode = "422",
          description = "Rejected: invalid card details, all errors listed (bank not called)",
          content = @Content),
      @ApiResponse(responseCode = "502", description = "Acquiring bank unavailable",
          content = @Content)
  })
  @PostMapping("/payment")
  public ResponseEntity<PostPaymentResponse> processPayment(
      @RequestBody PostPaymentRequest paymentRequest) {
    return new ResponseEntity<>(paymentGatewayService.processPayment(paymentRequest),
        HttpStatus.OK);
  }

  @Operation(summary = "Retrieve a payment",
      description = "Use the id returned by POST /payment. Only the last four card digits "
          + "are returned.")
  @GetMapping("/payment/{id}")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(
      @Parameter(example = "00000000-0000-0000-0000-000000000000") @PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }
}
