package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.tags.ElementTags;
import com.flipkart.krystal.tags.Tag;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public sealed interface VajramFacetDefinition permits DependencyDef, InputDef {

  String name();

  boolean isMandatory();

  String documentation();

  ElementTags tags();

  static ElementTags parseFacetTags(Field facetField) {
    return ElementTags.of(
        Arrays.stream(facetField.getAnnotations())
            .map(Tag::from)
            .collect(Collectors.toMap(Tag::tagKey, Function.identity())));
  }
}
