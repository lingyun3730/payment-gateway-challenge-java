package com.checkout.payment.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.bank.BankRequest;
import com.checkout.payment.gateway.bank.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.exception.PaymentRejectedException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.validation.PaymentValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentGatewayServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);

  private final BankClient bank = mock(BankClient.class);
  private PaymentsRepository repository;
  private PaymentGatewayService service;

  private final PostPaymentRequest validRequest =
      new PostPaymentRequest("2222405343248877", 4, 2030, "GBP", 100L, "123");

  @BeforeEach
  void setUp() {
    repository = new PaymentsRepository();
    service = new PaymentGatewayService(repository, new PaymentValidator(CLOCK), bank);
  }

  @Test
  void authorizedByBankIsReturnedAndStored() {
    when(bank.authorize(any())).thenReturn(new BankResponse(true, "code"));

    PostPaymentResponse payment = service.processPayment(validRequest);

    assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(payment.cardNumberLastFour()).isEqualTo("8877");
    assertThat(payment.expiryMonth()).isEqualTo(4);
    assertThat(payment.expiryYear()).isEqualTo(2030);
    assertThat(payment.currency()).isEqualTo("GBP");
    assertThat(payment.amount()).isEqualTo(100L);
    assertThat(repository.get(payment.id())).contains(payment);
  }

  @Test
  void declinedByBankIsReturnedAndStored() {
    when(bank.authorize(any())).thenReturn(new BankResponse(false, ""));

    PostPaymentResponse payment = service.processPayment(validRequest);

    assertThat(payment.status()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(repository.get(payment.id())).contains(payment);
  }

  @Test
  void bankReceivesTheFullCardDetails() {
    when(bank.authorize(any())).thenReturn(new BankResponse(true, "code"));

    service.processPayment(validRequest);

    ArgumentCaptor<BankRequest> sent = ArgumentCaptor.forClass(BankRequest.class);
    verify(bank).authorize(sent.capture());
    assertThat(sent.getValue()).isEqualTo(
        new BankRequest("2222405343248877", "04/2030", "GBP", 100L, "123"));
  }

  @Test
  void invalidRequestIsRejectedWithoutCallingTheBank() {
    PostPaymentRequest invalid = new PostPaymentRequest("123", 13, 2030, "GBP", 100L, "123");

    assertThatThrownBy(() -> service.processPayment(invalid))
        .isInstanceOfSatisfying(PaymentRejectedException.class,
            e -> assertThat(e.getErrors()).extracting("field")
                .containsExactly("cardNumber", "expiryMonth"));

    verifyNoInteractions(bank);
  }

  @Test
  void bankUnavailableIsNotStoredAndNotReportedAsDeclined() {
    when(bank.authorize(any())).thenThrow(new BankUnavailableException("down"));
    PaymentsRepository spyRepository = mock(PaymentsRepository.class);
    PaymentGatewayService serviceWithMockRepository =
        new PaymentGatewayService(spyRepository, new PaymentValidator(CLOCK), bank);

    assertThatThrownBy(() -> serviceWithMockRepository.processPayment(validRequest))
        .isInstanceOf(BankUnavailableException.class);

    verifyNoInteractions(spyRepository);
  }

  @Test
  void storedPaymentNeverContainsFullCardNumberOrCvv() {
    when(bank.authorize(any())).thenReturn(new BankResponse(true, "code"));

    PostPaymentResponse payment = service.processPayment(validRequest);

    assertThat(payment.toString()).doesNotContain("2222405343248877").doesNotContain("123");
  }

  @Test
  void unknownPaymentIdIsNotFound() {
    assertThatThrownBy(() -> service.getPaymentById(UUID.randomUUID()))
        .isInstanceOf(EventProcessingException.class);
  }
}
