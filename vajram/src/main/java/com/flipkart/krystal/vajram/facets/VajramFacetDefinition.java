package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.tags.ElementTags;

public sealed interface VajramFacetDefinition permits DependencyDef, InputDef {
  String name();

  boolean isMandatory();

  String documentation();

  ElementTags tags();
}
