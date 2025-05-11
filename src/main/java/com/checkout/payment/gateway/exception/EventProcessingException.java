package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;

public class EventProcessingException extends RuntimeException{
  private final HttpStatus status;

  public EventProcessingException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }

  // modified this to add more flexibility on exceptions
}
