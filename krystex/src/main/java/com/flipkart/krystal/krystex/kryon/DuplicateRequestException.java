package com.flipkart.krystal.krystex.kryon;

public class DuplicateRequestException extends RuntimeException {

  public DuplicateRequestException(String message) {
    super(message);
  }

  public DuplicateRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
