package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.PaymentRequestValidator;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final PaymentRequestValidator paymentRequestValidator;
  private final RestTemplate restTemplate;

  public PaymentGatewayService(
      PaymentsRepository paymentsRepository,
      PaymentRequestValidator paymentRequestValidator,
      RestTemplate restTemplate
  ) {
    this.paymentsRepository = paymentsRepository;
    this.paymentRequestValidator = paymentRequestValidator;
    this.restTemplate = restTemplate;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Payment not found", HttpStatus.NOT_FOUND));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    paymentRequestValidator.validatePaymentRequest(paymentRequest);

    BankPaymentRequest bankRequest = new BankPaymentRequest(
        paymentRequest.getCardNumber(),
        paymentRequest.getExpiryDate(),
        paymentRequest.getCurrency(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv()
    );

    PaymentStatus status;
    try {
      ResponseEntity<BankPaymentResponse> response = restTemplate.postForEntity(
          "http://localhost:8080/payments",
          bankRequest,
          BankPaymentResponse.class
      );

      if (response.getStatusCode().is2xxSuccessful()) {
        status = response.getBody().isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
      } else {
        throw new EventProcessingException("Bank error", HttpStatus.SERVICE_UNAVAILABLE);
      }

    } catch (RestClientException e) {
      throw new EventProcessingException("Bank unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    int lastFour = Integer.parseInt(paymentRequest.getCardNumber().substring(paymentRequest.getCardNumber().length() - 4));

    PostPaymentResponse result = new PostPaymentResponse();
    result.setId(UUID.randomUUID());
    result.setStatus(status);
    result.setCardNumberLastFour(lastFour);
    result.setExpiryMonth(paymentRequest.getExpiryMonth());
    result.setExpiryYear(paymentRequest.getExpiryYear());
    result.setCurrency(paymentRequest.getCurrency());
    result.setAmount(paymentRequest.getAmount());

    paymentsRepository.add(result);
    return result;
  }
}
