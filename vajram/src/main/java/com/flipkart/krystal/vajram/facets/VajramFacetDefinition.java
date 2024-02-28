package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.config.Tag;
import com.google.common.collect.ImmutableMap;

public sealed interface VajramFacetDefinition permits DependencyDef, InputDef {
  String name();

  boolean isMandatory();

  String documentation();

  ImmutableMap<Object, Tag> tags();
}
