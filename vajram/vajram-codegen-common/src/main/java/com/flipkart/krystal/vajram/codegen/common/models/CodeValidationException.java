package com.flipkart.krystal.vajram.codegen.common.models;

import java.io.Serial;

public class CodeValidationException extends RuntimeException {

  @Serial private static final long serialVersionUID = -5463456467467365L;

  CodeValidationException(String message) {
    super(message);
  }
}
