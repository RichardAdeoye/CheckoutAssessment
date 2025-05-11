package com.checkout.payment.gateway;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class PaymentGatewayIntegrationTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void shouldReturnAuthorizedWhenCardEndsInOddDigit() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111"); // ends in 1
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");

    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity(
        "/payments", request, PostPaymentResponse.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(PaymentStatus.AUTHORIZED, response.getBody().getStatus());
  }

  @Test
  void shouldReturnDeclinedWhenCardEndsInEvenDigit() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111112"); // ends in 2
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");

    ResponseEntity<PostPaymentResponse> response = restTemplate.postForEntity(
        "/payments", request, PostPaymentResponse.class
    );

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(PaymentStatus.DECLINED, response.getBody().getStatus());
  }

  @Test
  void shouldReturnServiceUnavailableWhenBankFails() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111110"); // ends in 0 → simulator returns 503
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");

    ResponseEntity<com.checkout.payment.gateway.model.ErrorResponse> response = restTemplate.postForEntity(
        "/payments", request, ErrorResponse.class
    );

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
  }
}

