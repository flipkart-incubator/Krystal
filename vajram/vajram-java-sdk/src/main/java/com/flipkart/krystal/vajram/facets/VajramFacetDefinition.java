package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.data.Facets;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;

public sealed interface VajramFacetDefinition permits DependencyDef, InputDef {
  int id();

  String name();

  boolean isMandatory();

  Function<? extends Facets, Object> getter();

  String documentation();

  ImmutableMap<Object, Tag> tags();
}
