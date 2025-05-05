package com.flipkart.krystal.vajram.codegen.common.models;

import java.io.Serial;

public class VajramValidationException extends CodeValidationException {

  @Serial private static final long serialVersionUID = -7618868579700286025L;

  VajramValidationException(String message) {
    super(message);
  }
}
