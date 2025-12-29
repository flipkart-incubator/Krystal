package com.flipkart.krystal.except;

public class SkippedExecutionException extends KrystalException {

  public SkippedExecutionException(String message) {
    super(message);
  }

  public static final SkippedExecutionException SKIPPED_EXECUTION_EXCEPTION =
      new SkippedExecutionException("Execution was skipped due to a skip command.");
}
