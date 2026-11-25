package com.checkout.payment.gateway.bank;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestBankClient implements BankClient {

  private static final Logger LOG = LoggerFactory.getLogger(RestBankClient.class);

  private final RestTemplate restTemplate;
  private final String bankUrl;

  public RestBankClient(RestTemplate restTemplate, @Value("${bank.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  @Override
  public BankResponse authorize(BankRequest request) {
    BankResponse response;
    try {
      response = restTemplate.postForObject(bankUrl, request, BankResponse.class);
    } catch (RestClientException e) {
      // Do not log the exception message or the request: they could contain card details.
      LOG.warn("Bank call failed: {}", e.getClass().getSimpleName());
      throw new BankUnavailableException("Acquiring bank is unavailable");
    }
    if (response == null) {
      throw new BankUnavailableException("Acquiring bank returned an empty response");
    }
    return response;
  }
}
