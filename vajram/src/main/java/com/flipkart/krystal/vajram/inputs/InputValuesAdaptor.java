package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.data.ValueOrError;

public interface InputValuesAdaptor {

  /**
   * @return The contents of this request as a map. Missing values are represented by {@link
   *     ValueOrError#empty()}
   */
  InputValues toInputValues();
}
