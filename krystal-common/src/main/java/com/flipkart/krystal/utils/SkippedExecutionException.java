package com.flipkart.krystal.utils;

public class SkippedExecutionException extends RuntimeException {

  public SkippedExecutionException(String message) {
    super(message);
  }

  @Override
  public Throwable fillInStackTrace() {
    return null;
  }
}
