package com.checkout.payment.gateway;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.YearMonth;
import java.util.Set;

@Component
public class PaymentRequestValidator {
  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP");

  public void validatePaymentRequest(PostPaymentRequest request) {
    if(request.getCardNumber() == null || !request.getCardNumber().matches("\\d{14,19}")) {
      throw new EventProcessingException(PaymentStatus.REJECTED.getName() + ": Invalid card number", HttpStatus.BAD_REQUEST);// rejected would be a bad request based on requirements
    }

    int expiryMonth = request.getExpiryMonth();

    if (expiryMonth < 1 || expiryMonth > 12) {
      throw new EventProcessingException(PaymentStatus.REJECTED.getName() + ": Invalid expiry month", HttpStatus.BAD_REQUEST);
    }

    YearMonth expiry = YearMonth.of(request.getExpiryYear(), expiryMonth);
    if (expiry.isBefore(YearMonth.now())) {
      throw new EventProcessingException(PaymentStatus.REJECTED.getName() + ": Card has expired", HttpStatus.BAD_REQUEST);
    }

    String cvv = request.getCvv();
    if (cvv == null || !cvv.matches("\\d{3,4}")) {
      throw new EventProcessingException(PaymentStatus.REJECTED.getName() + ": CVV must be 3 or 4 digits", HttpStatus.BAD_REQUEST);
    }

    String currency = request.getCurrency();
    if (currency == null || !SUPPORTED_CURRENCIES.contains(currency)) {
      throw new EventProcessingException(PaymentStatus.REJECTED.getName() + ": Unsupported currency " + currency, HttpStatus.BAD_REQUEST);
    }

    int amount = request.getAmount();
    if (amount <= 0) {
      throw new EventProcessingException(PaymentStatus.REJECTED.getName() + ": Amount must be greater than 0", HttpStatus.BAD_REQUEST);
    }
  }
}
