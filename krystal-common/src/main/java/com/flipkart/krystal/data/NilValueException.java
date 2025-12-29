package com.flipkart.krystal.data;

import com.flipkart.krystal.except.KrystalCompletionException;

public class NilValueException extends KrystalCompletionException {
  public NilValueException() {
    super("No value available in a Nil Errable");
  }
}
