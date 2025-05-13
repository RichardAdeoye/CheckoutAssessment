package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.GetPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/payment/{id}")
  public ResponseEntity<GetPaymentResponse> getPaymentById(@PathVariable UUID id) {
    GetPaymentResponse resp = paymentGatewayService.getPaymentById(id);
    return ResponseEntity.ok(resp);
  }

  @PostMapping("/payments")
  public ResponseEntity<PostPaymentResponse> processPayment(@RequestBody PostPaymentRequest request) {
    return ResponseEntity.ok(paymentGatewayService.processPayment(request));
  }
}
