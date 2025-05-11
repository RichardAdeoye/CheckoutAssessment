package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }


  @Test
  void whenValidPaymentRequestThenReturnAuthorized() throws Exception {
    String requestBody = """
         {
           "card_number": "4111111111111111",
           "expiry_month": 12,
           "expiry_year": 2026,
           "currency": "USD",
           "amount": 1050,
           "cvv": "123"
         }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1111"));
  }
  //We know if the card number ends in an odd number it will be authorized

  @Test
  void whenCardNumberEndsInEvenDigitThenReturnDeclined() throws Exception {
    String requestBody = """
    {
      "card_number": "4111111111111112",
      "expiry_month": 12,
      "expiry_year": 2026,
      "currency": "USD",
      "amount": 1050,
      "cvv": "123"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.cardNumberLastFour").value("1112"));
  }
//opposite if its even

  @Test
  void whenInvalidCardNumberThenReturnBadRequest() throws Exception {
    String requestBody = """
    {
      "card_number": "411",
      "expiry_month": 12,
      "expiry_year": 2026,
      "currency": "USD",
      "amount": 1050,
      "cvv": "123"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Rejected: Invalid card number"));
  }

  @Test
  void whenExpiryMonthIsInvalidThenReturnBadRequest() throws Exception {
    String requestBody = """
    {
      "card_number": "4111111111111111",
      "expiry_month": 13,
      "expiry_year": 2026,
      "currency": "USD",
      "amount": 1050,
      "cvv": "123"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected: Invalid expiry month"));
  }

  @Test
  void whenCardIsExpiredThenReturnBadRequest() throws Exception { // can try make sure the date is always in future dynamically
    String requestBody = """
    {
      "card_number": "4111111111111111",
      "expiry_month": 12,
      "expiry_year": 2020,
      "currency": "USD",
      "amount": 1050,
      "cvv": "123"
    }
    """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected: Card has expired"));
  }

  @Test
  void whenCvvIsInvalidThenReturnBadRequest() throws Exception {

    String requestBody = String.format("""
        {
          "card_number": "4111111111111111",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "USD",
          "amount": 1050,
          "cvv": "12"
        }
        """);

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void whenUnsupportedCurrencyThenReturnBadRequest() throws Exception {
    String requestBody = """
      {
        "card_number": "4111111111111111",
        "expiry_month": 12,
        "expiry_year": 2026,
        "currency": "JPY",
        "amount": 1050,
        "cvv": "123"
      }
      """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected: Unsupported currency JPY"));
  }

  @Test
  void whenAmountIsZeroOrNegativeThenReturnBadRequest() throws Exception {
    String requestBody = """
      {
        "card_number": "4111111111111111",
        "expiry_month": 12,
        "expiry_year": 2026,
        "currency": "USD",
        "amount": 0,
        "cvv": "123"
      }
      """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected: Amount must be greater than 0"));
  }

}
