package com.flipkart.krystal.data;

import com.flipkart.krystal.except.StackTracelessException;

public class NilValueException extends StackTracelessException {
  public NilValueException() {
    super("No value available in a Nil Errable");
  }
}
