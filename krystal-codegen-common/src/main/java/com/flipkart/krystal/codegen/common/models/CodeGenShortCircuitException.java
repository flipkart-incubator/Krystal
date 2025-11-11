package com.flipkart.krystal.codegen.common.models;

/**
 * An exception to signal that the code generation should be short-circuited. Exceptions of this
 * type do not generally mean something is wrong and that the code generation can be retried in the
 * next round and will most possibly succeed.
 */
public class CodeGenShortCircuitException extends RuntimeException {

  public CodeGenShortCircuitException(String message) {
    super(message);
  }
}
