package com.checkout.payment.gateway.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestBankClientTest {

  private static final String BANK_URL = "http://bank.test/payments";

  private MockRestServiceServer bank;
  private RestBankClient client;

  private final BankRequest request =
      new BankRequest("2222405343248877", "04/2030", "GBP", 100L, "123");

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    bank = MockRestServiceServer.createServer(restTemplate);
    client = new RestBankClient(restTemplate, BANK_URL);
  }

  @Test
  void authorizedResponseIsReturned() {
    bank.expect(requestTo(BANK_URL))
        .andRespond(withSuccess(
            "{\"authorized\": true, \"authorization_code\": \"abc-123\"}",
            MediaType.APPLICATION_JSON));

    BankResponse response = client.authorize(request);

    assertThat(response.authorized()).isTrue();
    assertThat(response.authorizationCode()).isEqualTo("abc-123");
  }

  @Test
  void declinedResponseIsReturned() {
    bank.expect(requestTo(BANK_URL))
        .andRespond(withSuccess(
            "{\"authorized\": false, \"authorization_code\": \"\"}",
            MediaType.APPLICATION_JSON));

    assertThat(client.authorize(request).authorized()).isFalse();
  }

  @Test
  void requestUsesTheBankFieldNames() {
    bank.expect(requestTo(BANK_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.card_number").value("2222405343248877"))
        .andExpect(jsonPath("$.expiry_date").value("04/2030"))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.cvv").value("123"))
        .andRespond(withSuccess(
            "{\"authorized\": true, \"authorization_code\": \"abc\"}",
            MediaType.APPLICATION_JSON));

    client.authorize(request);

    bank.verify();
  }

  @Test
  void serviceUnavailableBecomesBankUnavailable() {
    bank.expect(requestTo(BANK_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> client.authorize(request))
        .isInstanceOf(BankUnavailableException.class);
  }

  @Test
  void badRequestFromBankBecomesBankUnavailable() {
    bank.expect(requestTo(BANK_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> client.authorize(request))
        .isInstanceOf(BankUnavailableException.class);
  }

  @Test
  void connectionFailureBecomesBankUnavailable() {
    bank.expect(requestTo(BANK_URL)).andRespond(withException(new IOException("timed out")));

    assertThatThrownBy(() -> client.authorize(request))
        .isInstanceOf(BankUnavailableException.class);
  }

  @Test
  void emptyResponseBodyBecomesBankUnavailable() {
    bank.expect(requestTo(BANK_URL)).andRespond(withSuccess());

    assertThatThrownBy(() -> client.authorize(request))
        .isInstanceOf(BankUnavailableException.class);
  }

  @Test
  void errorMessageNeverContainsCardDetails() {
    bank.expect(requestTo(BANK_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> client.authorize(request))
        .hasMessageNotContaining("2222405343248877")
        .hasMessageNotContaining("123");
  }

  @Test
  void bankRequestToStringHidesCardDetails() {
    assertThat(request.toString())
        .doesNotContain("2222405343248877")
        .doesNotContain("cvv");
  }
}
