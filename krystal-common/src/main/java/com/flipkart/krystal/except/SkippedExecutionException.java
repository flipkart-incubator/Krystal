package com.flipkart.krystal.except;

public class SkippedExecutionException extends StackTracelessException {

  public SkippedExecutionException(String message) {
    super(message);
  }
}
