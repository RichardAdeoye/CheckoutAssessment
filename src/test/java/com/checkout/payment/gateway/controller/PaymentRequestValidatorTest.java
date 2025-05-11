
package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.PaymentRequestValidator;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestValidatorTest {

  private PaymentRequestValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PaymentRequestValidator();
  }

  @Test
  void shouldPassValidationForValidRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(YearMonth.now().getYear() + 1);
    request.setCurrency("USD");
    request.setAmount(1000);
    request.setCvv("123");
    assertDoesNotThrow(() -> validator.validatePaymentRequest(request));
  }

  @Test
  void shouldRejectInvalidCardNumber() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("123");
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("USD");
    request.setAmount(1000);
    request.setCvv("123");
    EventProcessingException ex = assertThrows(EventProcessingException.class, () -> validator.validatePaymentRequest(request));
    assertEquals("Rejected: Invalid card number", ex.getMessage());
  }

  @Test
  void shouldRejectInvalidExpiryMonth() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(13);
    request.setExpiryYear(2026);
    request.setCurrency("USD");
    request.setAmount(1000);
    request.setCvv("123");
    EventProcessingException ex = assertThrows(EventProcessingException.class, () -> validator.validatePaymentRequest(request));
    assertEquals("Rejected: Invalid expiry month", ex.getMessage());
  }

  @Test
  void shouldRejectExpiredCard() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(1);
    request.setExpiryYear(2020);
    request.setCurrency("USD");
    request.setAmount(1000);
    request.setCvv("123");
    EventProcessingException ex = assertThrows(EventProcessingException.class, () -> validator.validatePaymentRequest(request));
    assertEquals("Rejected: Card has expired", ex.getMessage());
  }

  @Test
  void shouldRejectInvalidCvv() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("USD");
    request.setAmount(1000);
    request.setCvv("12");
    EventProcessingException ex = assertThrows(EventProcessingException.class, () -> validator.validatePaymentRequest(request));
    assertEquals("Rejected: CVV must be 3 or 4 digits", ex.getMessage());
  }

  @Test
  void shouldRejectUnsupportedCurrency() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("JPY");
    request.setAmount(1000);
    request.setCvv("123");
    EventProcessingException ex = assertThrows(EventProcessingException.class, () -> validator.validatePaymentRequest(request));
    assertEquals("Rejected: Unsupported currency JPY", ex.getMessage());
  }

  @Test
  void shouldRejectZeroAmount() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("USD");
    request.setAmount(0);
    request.setCvv("123");
    EventProcessingException ex = assertThrows(EventProcessingException.class, () -> validator.validatePaymentRequest(request));
    assertEquals("Rejected: Amount must be greater than 0", ex.getMessage());
  }
}