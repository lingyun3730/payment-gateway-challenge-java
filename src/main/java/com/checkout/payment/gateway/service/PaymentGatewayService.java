package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.bank.BankRequest;
import com.checkout.payment.gateway.bank.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentRejectedException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.model.RejectedResponse.FieldError;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentValidator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final PaymentValidator validator;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, PaymentValidator validator,
      BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.validator = validator;
    this.bankClient = bankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  /**
   * Validates the request, asks the bank, and stores the outcome. If the bank gives no answer, an
   * exception is thrown and nothing is stored.
   */
  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    List<FieldError> errors = validator.validate(paymentRequest);
    if (!errors.isEmpty()) {
      throw new PaymentRejectedException(errors);
    }

    BankResponse bankResponse = bankClient.authorize(BankRequest.from(paymentRequest));
    PaymentStatus status = bankResponse.authorized()
        ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

    PostPaymentResponse payment = new PostPaymentResponse(
        UUID.randomUUID(),
        status,
        lastFourDigits(paymentRequest.cardNumber()),
        paymentRequest.expiryMonth(),
        paymentRequest.expiryYear(),
        paymentRequest.currency(),
        paymentRequest.amount().longValueExact());
    paymentsRepository.add(payment);

    LOG.info("Payment {} {}", payment.id(), status);
    return payment;
  }

  private static String lastFourDigits(String cardNumber) {
    return cardNumber.substring(cardNumber.length() - 4);
  }
}
