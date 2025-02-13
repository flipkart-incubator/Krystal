package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.google.common.collect.ImmutableSet;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface FacetGenModel permits GivenFacetModel, DependencyModel {
  int id();

  String name();

  VajramInfoLite vajramInfo();

  @Nullable Mandatory mandatoryAnno();

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

  default boolean isMandatory() {
    return mandatoryAnno() != null;
  }

  @Nullable String documentation();
}
