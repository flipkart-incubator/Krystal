package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.google.common.collect.ImmutableSet;
import javax.lang.model.element.VariableElement;

public sealed interface FacetGenModel permits GivenFacetModel, DependencyModel {
  int id();

  String name();

  VajramInfoLite vajramInfo();

  boolean isMandatory();

  VariableElement facetField();

  ImmutableSet<FacetType> facetTypes();

  DataType<?> dataType();

  boolean isBatched();

  default boolean isOptional() {
    return !isMandatory();
  }

  default boolean isGiven() {
    return false;
  }
}
