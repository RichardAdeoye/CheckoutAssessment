package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.PaymentRequestValidator;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private PaymentRequestValidator validator;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private PaymentGatewayService service;

  @Test
  void shouldReturnAuthorizedWhenBankApproves() {
    // Arrange
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111111");
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");

    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "auth-code-xyz");

    when(restTemplate.postForEntity(
        eq("http://localhost:8080/payments"),
        any(),
        eq(BankPaymentResponse.class)
    )).thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    // Act
    PostPaymentResponse response = service.processPayment(request);

    // Assert
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals(1111, response.getCardNumberLastFour());
    assertEquals("GBP", response.getCurrency());
    assertEquals(1050, response.getAmount());
  }

  @Test
  void shouldReturnDeclinedWhenBankDenies() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111112"); // ends in even digit
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");

    BankPaymentResponse bankResponse = new BankPaymentResponse(false, "auth-code-xyz");

    when(restTemplate.postForEntity(
        eq("http://localhost:8080/payments"),
        any(),
        eq(BankPaymentResponse.class)
    )).thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    PostPaymentResponse response = service.processPayment(request);

    assertEquals(PaymentStatus.DECLINED, response.getStatus());
  }

  @Test
  void shouldThrowExceptionWhenBankUnavailable() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4111111111111110"); // ends in 0
    request.setExpiryMonth(12);
    request.setExpiryYear(2026);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");

    when(restTemplate.postForEntity(
        eq("http://localhost:8080/payments"),
        any(),
        eq(BankPaymentResponse.class)
    )).thenThrow(new RestClientException("Service Unavailable"));

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> service.processPayment(request)
    );

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
  }

  @Test
  void shouldRetrieveStoredPaymentById() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse stored = new PostPaymentResponse();
    stored.setId(id);
    stored.setStatus(PaymentStatus.AUTHORIZED);
    stored.setCardNumberLastFour(1111);
    stored.setCurrency("GBP");
    stored.setAmount(1050);

    when(paymentsRepository.get(id)).thenReturn(Optional.of(stored));

    PostPaymentResponse result = service.getPaymentById(id);

    assertEquals(id, result.getId());
    assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
  }

  @Test
  void shouldThrowNotFoundWhenPaymentMissing() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> service.getPaymentById(id)
    );

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
  }

}

