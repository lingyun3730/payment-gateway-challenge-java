package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.RejectedResponse.FieldError;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Checks a payment request and collects every problem, so the merchant can fix them all in one go.
 * Error messages describe the rule only. They never repeat the submitted value, because it may
 * be a card number or CVV.
 */
@Component
public class PaymentValidator {

  private static final Set<String> ISO_CURRENCY_CODES = Currency.getAvailableCurrencies().stream()
      .map(Currency::getCurrencyCode)
      .collect(Collectors.toUnmodifiableSet());

  private final Clock clock;

  public PaymentValidator(Clock clock) {
    this.clock = clock;
  }

  public List<FieldError> validate(PostPaymentRequest request) {
    List<FieldError> errors = new ArrayList<>();

    checkCardNumber(request.cardNumber(), errors);
    boolean monthIsValid = checkExpiryMonth(request.expiryMonth(), errors);
    boolean yearIsValid = checkExpiryYear(request.expiryYear(), errors);
    if (monthIsValid && yearIsValid) {
      checkExpiryIsInFuture(request.expiryMonth(), request.expiryYear(), errors);
    }
    checkCurrency(request.currency(), errors);
    checkAmount(request.amount(), errors);
    checkCvv(request.cvv(), errors);

    return errors;
  }

  private void checkCardNumber(String cardNumber, List<FieldError> errors) {
    if (cardNumber == null) {
      errors.add(required("cardNumber"));
    } else if (!isDigitsWithLength(cardNumber, 14, 19)) {
      errors.add(new FieldError("cardNumber", "must be 14-19 numeric characters"));
    }
  }

  private boolean checkExpiryMonth(Integer month, List<FieldError> errors) {
    if (month == null) {
      errors.add(required("expiryMonth"));
      return false;
    }
    if (month < 1 || month > 12) {
      errors.add(new FieldError("expiryMonth", "must be between 1 and 12"));
      return false;
    }
    return true;
  }

  private boolean checkExpiryYear(Integer year, List<FieldError> errors) {
    if (year == null) {
      errors.add(required("expiryYear"));
      return false;
    }
    if (year < 1000 || year > 9999) {
      errors.add(new FieldError("expiryYear", "must be a four-digit year"));
      return false;
    }
    return true;
  }

  // A card stays valid until the end of its expiry month.
  private void checkExpiryIsInFuture(int month, int year, List<FieldError> errors) {
    YearMonth expiry = YearMonth.of(year, month);
    if (expiry.isBefore(YearMonth.now(clock))) {
      errors.add(new FieldError("expiryYear", "expiry month and year must be in the future"));
    }
  }

  private void checkCurrency(String currency, List<FieldError> errors) {
    if (currency == null) {
      errors.add(required("currency"));
    } else if (!ISO_CURRENCY_CODES.contains(currency)) {
      errors.add(new FieldError("currency", "must be a valid three-letter ISO 4217 code"));
    }
  }

  private void checkAmount(BigDecimal amount, List<FieldError> errors) {
    if (amount == null) {
      errors.add(required("amount"));
    } else if (!isWholeNumberInLongRange(amount)) {
      errors.add(new FieldError("amount", "must be a whole number in the minor currency unit"));
    } else if (amount.signum() <= 0) {
      errors.add(new FieldError("amount", "must be greater than 0 in the minor currency unit"));
    }
  }

  private static boolean isWholeNumberInLongRange(BigDecimal amount) {
    try {
      amount.longValueExact();
      return true;
    } catch (ArithmeticException e) {
      return false;
    }
  }

  private void checkCvv(String cvv, List<FieldError> errors) {
    if (cvv == null) {
      errors.add(required("cvv"));
    } else if (!isDigitsWithLength(cvv, 3, 4)) {
      errors.add(new FieldError("cvv", "must be 3-4 numeric characters"));
    }
  }

  private static boolean isDigitsWithLength(String value, int min, int max) {
    return value.length() >= min && value.length() <= max && value.chars().allMatch(
        c -> c >= '0' && c <= '9');
  }

  private static FieldError required(String field) {
    return new FieldError(field, "is required");
  }
}
