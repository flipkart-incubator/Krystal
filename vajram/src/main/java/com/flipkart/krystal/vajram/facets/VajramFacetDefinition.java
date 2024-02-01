package com.flipkart.krystal.vajram.facets;

public sealed interface VajramFacetDefinition permits DependencyDef, InputDef {
  String name();

  boolean isMandatory();

  String documentation();
}
