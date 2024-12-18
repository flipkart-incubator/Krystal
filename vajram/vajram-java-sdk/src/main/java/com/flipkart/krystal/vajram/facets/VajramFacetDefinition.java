package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.tags.ElementTags;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Function;

public sealed interface VajramFacetDefinition permits DependencyDef, InputDef {

  int id();

  String name();

  boolean isMandatory();

  String documentation();

  ElementTags tags();

  FacetValueSetter setter();

  FacetValueGetter getter();

  static ElementTags parseFacetTags(Field facetField) {
    return ElementTags.of(Arrays.stream(facetField.getAnnotations()).toList());
  }
}
