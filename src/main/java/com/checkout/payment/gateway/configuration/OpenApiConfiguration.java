package com.checkout.payment.gateway.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI only: request examples for the "Examples" drop-down and the documented error
 * responses of POST /payment. Keeps the controller free of documentation annotations.
 */
@Configuration
public class OpenApiConfiguration {

  private static final String CARD_OK = "2222405343248877";

  @Bean
  public OpenApiCustomizer paymentDocs(ObjectMapper objectMapper) {
    return openApi -> {
      var post = openApi.getPaths().get("/payment").getPost();
      post.summary("Process a card payment");
      post.description("Card number ending in an odd digit is authorized, an even digit is "
          + "declined, and 0 makes the bank simulator return 503.");

      var json = post.getRequestBody().getContent().get("application/json");
      json.setExamples(examples(objectMapper));

      post.getResponses()
          .addApiResponse("200", new ApiResponse()
              .description("Bank answered: status is Authorized or Declined")
              .content(post.getResponses().get("200").getContent()))
          .addApiResponse("400", new ApiResponse()
              .description("Malformed JSON or wrong field type"))
          .addApiResponse("422", new ApiResponse()
              .description("Rejected: invalid card details, all errors listed (bank not called)"))
          .addApiResponse("502", new ApiResponse().description("Acquiring bank unavailable"));
    };
  }

  private Map<String, Example> examples(ObjectMapper objectMapper) {
    Map<String, Example> examples = new LinkedHashMap<>();
    examples.put("1. Authorized (200)", example(objectMapper, body(CARD_OK, 4, 2035, "GBP", 100, "123")));
    examples.put("2. Declined (200)", example(objectMapper, body("2222405343248878", 4, 2035, "USD", 2500, "123")));
    examples.put("3. Bank unavailable (502)", example(objectMapper, body("2222405343248870", 4, 2035, "EUR", 100, "123")));
    examples.put("4. Rejected: several invalid fields (422)", example(objectMapper, body("12", 13, 2020, "GBP", 0, "1")));
    examples.put("5. Rejected: fields missing (422)", example(objectMapper, "{}"));
    examples.put("6. Rejected: card expired (422)", example(objectMapper, body(CARD_OK, 1, 2020, "GBP", 100, "123")));
    examples.put("7. Malformed JSON (400)", new Example().value("{oops"));
    examples.put("8. Amount is not an integer (400)", example(objectMapper, body(CARD_OK, 4, 2035, "GBP", 10.5, "123")));
    return examples;
  }

  private String body(String card, int month, int year, String currency, Number amount,
      String cvv) {
    return String.format(
        "{\"cardNumber\":\"%s\",\"expiryMonth\":%d,\"expiryYear\":%d,"
            + "\"currency\":\"%s\",\"amount\":%s,\"cvv\":\"%s\"}",
        card, month, year, currency, amount, cvv);
  }

  private Example example(ObjectMapper objectMapper, String json) {
    try {
      return new Example().value(objectMapper.readTree(json));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
