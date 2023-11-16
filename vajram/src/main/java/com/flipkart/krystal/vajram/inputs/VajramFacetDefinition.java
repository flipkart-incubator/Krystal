package com.flipkart.krystal.vajram.inputs;

public sealed interface VajramFacetDefinition permits Dependency, Input {
  String name();

  boolean isMandatory();

  String documentation();
}
