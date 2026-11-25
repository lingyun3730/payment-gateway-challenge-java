package com.checkout.payment.gateway.controller;

/** Request bodies offered in the Swagger UI "Examples" drop-down. They only feed the API docs. */
final class PaymentExamples {

  private PaymentExamples() {
  }

  static final String AUTHORIZED = """
      {"cardNumber":"2222405343248877","expiryMonth":4,"expiryYear":2035,
       "currency":"GBP","amount":100,"cvv":"123"}""";

  static final String DECLINED = """
      {"cardNumber":"2222405343248878","expiryMonth":4,"expiryYear":2035,
       "currency":"USD","amount":2500,"cvv":"123"}""";

  static final String BANK_UNAVAILABLE = """
      {"cardNumber":"2222405343248870","expiryMonth":4,"expiryYear":2035,
       "currency":"EUR","amount":100,"cvv":"123"}""";

  static final String SEVERAL_INVALID_FIELDS = """
      {"cardNumber":"12","expiryMonth":13,"expiryYear":2020,
       "currency":"GBP","amount":0,"cvv":"1"}""";

  static final String MISSING_FIELDS = "{}";

  static final String EXPIRED_CARD = """
      {"cardNumber":"2222405343248877","expiryMonth":1,"expiryYear":2020,
       "currency":"GBP","amount":100,"cvv":"123"}""";

  static final String MALFORMED_JSON = "{oops";

  static final String AMOUNT_NOT_AN_INTEGER = """
      {"cardNumber":"2222405343248877","expiryMonth":4,"expiryYear":2035,
       "currency":"GBP","amount":10.5,"cvv":"123"}""";
}
