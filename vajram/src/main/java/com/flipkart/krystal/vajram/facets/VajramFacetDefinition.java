package com.flipkart.krystal.vajram.facets;

public sealed interface VajramFacetDefinition permits Dependency, Input {
  String name();

  boolean isMandatory();

  String documentation();
}
