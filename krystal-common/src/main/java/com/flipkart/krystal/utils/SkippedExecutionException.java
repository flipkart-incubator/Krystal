package com.flipkart.krystal.utils;

import com.flipkart.krystal.except.StackTracelessException;

public class SkippedExecutionException extends StackTracelessException {

  public SkippedExecutionException(String message) {
    super(message);
  }
}
