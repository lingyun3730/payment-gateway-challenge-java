package com.checkout.payment.gateway.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.RejectedResponse.FieldError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentValidatorTest {

  // "Now" is fixed at 15 Nov 2026 so expiry tests do not depend on the real date.
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);

  private final PaymentValidator validator = new PaymentValidator(CLOCK);

  private static PostPaymentRequest request(String cardNumber, Integer month, Integer year,
      String currency, Long amount, String cvv) {
    return new PostPaymentRequest(cardNumber, month, year, currency, amount, cvv);
  }

  private static PostPaymentRequest validRequest() {
    return request("2222405343248877", 4, 2030, "GBP", 100L, "123");
  }

  private static List<String> failedFields(List<FieldError> errors) {
    return errors.stream().map(FieldError::field).toList();
  }

  @Test
  void validRequestHasNoErrors() {
    assertThat(validator.validate(validRequest())).isEmpty();
  }

  @Test
  void allErrorsAreReportedAtOnce() {
    PostPaymentRequest empty = request(null, null, null, null, null, null);

    assertThat(failedFields(validator.validate(empty)))
        .containsExactlyInAnyOrder("cardNumber", "expiryMonth", "expiryYear", "currency",
            "amount", "cvv");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"1234567890123", "12345678901234567890", "2222abcd43248877",
      "2222 4053 4324 8877", ""})
  void invalidCardNumberIsRejected(String cardNumber) {
    PostPaymentRequest request = request(cardNumber, 4, 2030, "GBP", 100L, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("cardNumber");
  }

  @ParameterizedTest
  @ValueSource(strings = {"12345678901234", "1234567890123456789"})
  void cardNumberLengthBoundariesAreAccepted(String cardNumber) {
    PostPaymentRequest request = request(cardNumber, 4, 2030, "GBP", 100L, "123");

    assertThat(validator.validate(request)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 13, -1})
  void invalidExpiryMonthIsRejected(int month) {
    PostPaymentRequest request = request("2222405343248877", month, 2030, "GBP", 100L, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("expiryMonth");
  }

  @Test
  void expiryInPreviousMonthIsRejected() {
    PostPaymentRequest request = request("2222405343248877", 10, 2026, "GBP", 100L, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("expiryYear");
  }

  @Test
  void expiryInCurrentMonthIsAccepted() {
    PostPaymentRequest request = request("2222405343248877", 11, 2026, "GBP", 100L, "123");

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void expiryInNextMonthIsAccepted() {
    PostPaymentRequest request = request("2222405343248877", 12, 2026, "GBP", 100L, "123");

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void expiryInPastYearIsRejected() {
    PostPaymentRequest request = request("2222405343248877", 12, 2025, "GBP", 100L, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("expiryYear");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 26, 202, 20300})
  void yearThatIsNotFourDigitsIsRejected(int year) {
    PostPaymentRequest request = request("2222405343248877", 4, year, "GBP", 100L, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("expiryYear");
  }

  @Test
  void invalidMonthDoesNotAlsoReportExpiryYear() {
    PostPaymentRequest request = request("2222405343248877", 13, 2020, "GBP", 100L, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("expiryMonth");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "US", "USDD", "usd", "ABC", "12A"})
  void invalidCurrencyIsRejected(String currency) {
    PostPaymentRequest request = request("2222405343248877", 4, 2030, currency, 100L, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("currency");
  }

  @ParameterizedTest
  @ValueSource(strings = {"USD", "GBP", "EUR", "JPY", "CNY"})
  void anyIsoCurrencyIsAccepted(String currency) {
    PostPaymentRequest request = request("2222405343248877", 4, 2030, currency, 100L, "123");

    assertThat(validator.validate(request)).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1})
  void amountMustBePositive(long amount) {
    PostPaymentRequest request = request("2222405343248877", 4, 2030, "GBP", amount, "123");

    assertThat(failedFields(validator.validate(request))).containsExactly("amount");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "12", "12345", "12a", "1 2"})
  void invalidCvvIsRejected(String cvv) {
    PostPaymentRequest request = request("2222405343248877", 4, 2030, "GBP", 100L, cvv);

    assertThat(failedFields(validator.validate(request))).containsExactly("cvv");
  }

  @ParameterizedTest
  @ValueSource(strings = {"123", "1234", "012"})
  void cvvOfThreeOrFourDigitsIsAccepted(String cvv) {
    PostPaymentRequest request = request("2222405343248877", 4, 2030, "GBP", 100L, cvv);

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void errorMessagesNeverContainTheSubmittedValues() {
    PostPaymentRequest request = request("1234abcd", 4, 2030, "GBP", 100L, "99x");

    String messages = validator.validate(request).toString();

    assertThat(messages).doesNotContain("1234abcd").doesNotContain("99x");
  }
}
