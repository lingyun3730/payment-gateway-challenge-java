package com.checkout.payment.gateway.controller;


import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.bank.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Year;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  private static final String CARD_NUMBER = "2222405343248877";

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  @MockBean
  private BankClient bankClient;

  private static String paymentJson(String cardNumber, String month, String year,
      String currency, String amount, String cvv) {
    return "{\"cardNumber\":" + cardNumber + ",\"expiryMonth\":" + month
        + ",\"expiryYear\":" + year + ",\"currency\":" + currency
        + ",\"amount\":" + amount + ",\"cvv\":" + cvv + "}";
  }

  private static String validPaymentJson() {
    String year = String.valueOf(Year.now().getValue() + 2);
    return paymentJson("\"" + CARD_NUMBER + "\"", "4", year, "\"GBP\"", "100", "\"123\"");
  }

  private ResultActions post(String body) throws Exception {
    return mvc.perform(MockMvcRequestBuilders.post("/payment")
        .contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  void authorizedPaymentReturns200AndCanBeRetrieved() throws Exception {
    when(bankClient.authorize(any())).thenReturn(new BankResponse(true, "code"));

    MvcResult result = post(validPaymentJson())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("8877"))
        .andExpect(jsonPath("$.expiryMonth").value(4))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.cardNumber").doesNotExist())
        .andExpect(jsonPath("$.cvv").doesNotExist())
        .andReturn();

    String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("8877"));
  }

  @Test
  void declinedPaymentReturns200() throws Exception {
    when(bankClient.authorize(any())).thenReturn(new BankResponse(false, ""));

    post(validPaymentJson())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void invalidPaymentReturns422WithAllErrorsAndDoesNotCallTheBank() throws Exception {
    String body = paymentJson("\"123\"", "13", "2020", "\"XYZ\"", "100", "\"1\"");

    post(body)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.errors.length()").value(4))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(content().string(not(containsString("123"))));

    verifyNoInteractions(bankClient);
  }

  @Test
  void missingFieldsReturn422() throws Exception {
    post("{}")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors.length()").value(6));
  }

  @Test
  void bankUnavailableReturns502() throws Exception {
    when(bankClient.authorize(any())).thenThrow(new BankUnavailableException("down"));

    post(validPaymentJson())
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Acquiring bank is unavailable"));
  }

  @Test
  void malformedJsonReturns400AndDoesNotCallTheBank() throws Exception {
    post("{not json").andExpect(status().isBadRequest());
    post("").andExpect(status().isBadRequest());

    verifyNoInteractions(bankClient);
  }

  @Test
  void amountThatIsNotAnIntegerReturns400() throws Exception {
    String year = String.valueOf(Year.now().getValue() + 2);

    post(paymentJson("\"" + CARD_NUMBER + "\"", "4", year, "\"GBP\"", "10.5", "\"123\""))
        .andExpect(status().isBadRequest());
    post(paymentJson("\"" + CARD_NUMBER + "\"", "4", year, "\"GBP\"", "\"100\"", "\"123\""))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(bankClient);
  }

  @Test
  void malformedBodyErrorDoesNotEchoCardDetails() throws Exception {
    String year = String.valueOf(Year.now().getValue() + 2);

    post(paymentJson("\"" + CARD_NUMBER + "\"", "4", year, "\"GBP\"", "10.5", "\"123\""))
        .andExpect(content().string(not(containsString(CARD_NUMBER))));
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse(
        UUID.randomUUID(), PaymentStatus.AUTHORIZED, "4321", 12, 2024, "USD", 10);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.id()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.status().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.cardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.expiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.expiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.currency()))
        .andExpect(jsonPath("$.amount").value(payment.amount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }
}
