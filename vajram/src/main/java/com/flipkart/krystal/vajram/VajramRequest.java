package com.flipkart.krystal.vajram;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public interface VajramRequest {

  /**
   * @return The contents of this request as a map. Missing values are represented by {@link
   *     Optional#empty()}
   */
  ImmutableMap<String, Optional<Object>> asMap();
}
