package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.modulation.InputsConverter;

public abstract non-sealed class IOVajram<T> extends AbstractVajram<T> {

  public InputsConverter<?, ?> getInputsConvertor() {
    throw new UnsupportedOperationException(
        "getInputsConvertor method should be implemented by an IOVajram");
  }
}
