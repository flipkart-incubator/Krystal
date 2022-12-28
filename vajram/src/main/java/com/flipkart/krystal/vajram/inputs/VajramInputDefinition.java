package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.datatypes.DataType;

public sealed interface VajramInputDefinition<T> permits Dependency, Input {
  String name();

  boolean isMandatory();

  default boolean isOptional() {
    return !isMandatory();
  }

  boolean needsModulation();

  DataType<? extends T> type();
}
