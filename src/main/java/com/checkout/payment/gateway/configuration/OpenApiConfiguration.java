package com.checkout.payment.gateway.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Swagger UI only: request examples for the "Examples" drop-down (bodies live in
 * resources/openapi-examples) and the documented error responses of POST /payment.
 */
@Configuration
public class OpenApiConfiguration {

  private static final String EXAMPLES_DIR = "openapi-examples/";

  @Bean
  public OpenApiCustomizer paymentDocs(ObjectMapper objectMapper) {
    return openApi -> {
      var post = openApi.getPaths().get("/payment").getPost();
      post.summary("Process a card payment");
      post.description("Card number ending in an odd digit is authorized, an even digit is "
          + "declined, and 0 makes the bank simulator return 503.");

      post.getRequestBody().getContent().get("application/json")
          .setExamples(examples(objectMapper));

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
    examples.put("1. Authorized (200)", load(objectMapper, "1-authorized.json"));
    examples.put("2. Declined (200)", load(objectMapper, "2-declined.json"));
    examples.put("3. Bank unavailable (502)", load(objectMapper, "3-bank-unavailable.json"));
    examples.put("4. Rejected: several invalid fields (422)",
        load(objectMapper, "4-several-invalid-fields.json"));
    examples.put("5. Rejected: fields missing (422)", load(objectMapper, "5-missing-fields.json"));
    examples.put("6. Rejected: card expired (422)", load(objectMapper, "6-expired-card.json"));
    examples.put("7. Malformed JSON (400)", load(objectMapper, "7-malformed-json.json"));
    examples.put("8. Amount is not an integer (400)",
        load(objectMapper, "8-amount-not-integer.json"));
    return examples;
  }

  private Example load(ObjectMapper objectMapper, String file) {
    try {
      String text = new ClassPathResource(EXAMPLES_DIR + file)
          .getContentAsString(StandardCharsets.UTF_8).trim();
      try {
        return new Example().value(objectMapper.readTree(text));
      } catch (JsonProcessingException notJson) {
        // The "malformed JSON" example is intentionally not valid JSON.
        return new Example().value(text);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
