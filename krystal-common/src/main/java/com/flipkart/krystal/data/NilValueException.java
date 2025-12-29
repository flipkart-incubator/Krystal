package com.flipkart.krystal.data;

import com.flipkart.krystal.except.KrystalException;

public class NilValueException extends KrystalException {
  public NilValueException() {
    super("No value available in a Nil Errable");
  }
}
