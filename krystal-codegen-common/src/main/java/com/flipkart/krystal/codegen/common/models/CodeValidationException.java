package com.flipkart.krystal.codegen.common.models;

import java.io.Serial;

public class CodeValidationException extends RuntimeException {

  @Serial private static final long serialVersionUID = -5463456467467365L;

  public CodeValidationException(String message) {
    super(message);
  }
}
