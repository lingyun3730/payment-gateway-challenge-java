package com.checkout.payment.gateway.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PostPaymentRequestTest {

  @Test
  void toStringNeverShowsTheCardNumberOrCvv() {
    PostPaymentRequest request = new PostPaymentRequest(
        "2222405343248877", 4, 2030, "GBP", BigDecimal.valueOf(100), "123");

    String text = request.toString();

    assertThat(text).doesNotContain("2222405343248877", "8877", "123", "cvv");
    assertThat(text).contains("GBP", "100");
  }
}
