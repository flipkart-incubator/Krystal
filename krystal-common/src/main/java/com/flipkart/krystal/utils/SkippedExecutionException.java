package com.flipkart.krystal.utils;

import com.flipkart.krystal.except.StackTracelessException;

public class SkippedExecutionException extends StackTracelessException {

  public SkippedExecutionException(String message) {
    super(message);
  }

  public static final SkippedExecutionException SKIPPED_EXECUTION_EXCEPTION =
      new SkippedExecutionException("Execution was skipped due to a skip command.");
}
