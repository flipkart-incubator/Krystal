package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.ValueOrError;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public interface VajramInputs {

  /**
   * @return The contents of this request as a map. Missing values are represented by {@link
   *     Optional#empty()}
   */
  ImmutableMap<String, ValueOrError<?>> asMap();
}
