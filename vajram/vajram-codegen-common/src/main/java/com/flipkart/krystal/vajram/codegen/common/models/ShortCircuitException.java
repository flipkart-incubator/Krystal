package com.flipkart.krystal.vajram.codegen.common.models;

/**
 * An exception to signal that the code generation should be short-circuited. Exceptions of this
 * type do not generally mean something is wrong.
 */
public abstract class ShortCircuitException extends RuntimeException {

  public ShortCircuitException(String message) {
    super(message);
  }
}
