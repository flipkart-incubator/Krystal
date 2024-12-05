package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.tags.ElementTags;
import java.lang.reflect.Field;
import java.util.Arrays;

public sealed interface VajramFacetDefinition permits DependencyDef, InputDef {

  String name();

  boolean isMandatory();

  String documentation();

  ElementTags tags();

  static ElementTags parseFacetTags(Field facetField) {
    return ElementTags.of(Arrays.stream(facetField.getAnnotations()).toList());
  }
}
