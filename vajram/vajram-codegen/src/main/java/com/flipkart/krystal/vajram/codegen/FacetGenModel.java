package com.flipkart.krystal.vajram.codegen;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface FacetGenModel permits GivenFacetModel, DependencyModel {
  int id();

  String name();

  VajramInfoLite vajramInfo();

  VariableElement facetField();

  ImmutableSet<FacetType> facetTypes();

  DataType<?> dataType();

  default boolean isOptional() {
    return !isMandatory();
  }

  default boolean isGiven() {
    return false;
  }

  default boolean isMandatory() {
    return facetField().getAnnotation(Mandatory.class) != null;
  }

  @Nullable String documentation();

  default boolean isBatched() {
    return facetField().getAnnotation(Batched.class) != null;
  }

  default boolean isUsedToGroupBatches() {
    return facetField().getAnnotation(BatchesGroupedBy.class) != null;
  }

  @SneakyThrows
  public default List<? extends AnnotationMirror> annotations() {
    return facetField().getAnnotationMirrors();
  }
}
