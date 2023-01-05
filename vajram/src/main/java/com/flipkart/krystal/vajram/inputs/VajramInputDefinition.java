package com.flipkart.krystal.vajram.inputs;

public sealed interface VajramInputDefinition permits Dependency, Input {
  String name();

  boolean isMandatory();

  default boolean isOptional() {
    return !isMandatory();
  }

  boolean needsModulation();

  String documentation();
}
