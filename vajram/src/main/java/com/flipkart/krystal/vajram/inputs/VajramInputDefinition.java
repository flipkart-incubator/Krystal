package com.flipkart.krystal.vajram.inputs;

public sealed interface VajramInputDefinition permits Dependency, Input {
  String name();

  boolean isMandatory();

  boolean needsModulation();
}
