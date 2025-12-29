package com.flipkart.krystal.except;

public class SkippedExecutionException extends KrystalCompletionException {

  public SkippedExecutionException(String message) {
    super(message);
  }
}
