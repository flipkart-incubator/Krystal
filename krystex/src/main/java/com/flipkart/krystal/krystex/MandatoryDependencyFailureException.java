package com.flipkart.krystal.krystex;

import java.util.Map;

public class MandatoryDependencyFailureException extends RuntimeException {

  public MandatoryDependencyFailureException(Map<String, Throwable> failedMandatoryDeps) {

  }
}
