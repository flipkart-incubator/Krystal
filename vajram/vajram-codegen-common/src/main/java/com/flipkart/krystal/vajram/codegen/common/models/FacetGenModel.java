package com.flipkart.krystal.vajram.codegen.common.models;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface FacetGenModel permits DefaultFacetModel, DependencyModel {
  int id();

  String name();

  VajramInfoLite vajramInfo();

  VariableElement facetField();

  ImmutableSet<FacetType> facetTypes();

  CodeGenType dataType();

  default boolean isGiven() {
    return false;
  }

  default boolean isMandatoryOnServer() {
    IfAbsent ifAbsent = facetField().getAnnotation(IfAbsent.class);
    return ifAbsent != null && ifAbsent.value().isMandatoryOnServer();
  }

  @Nullable String documentation();

  default boolean isBatched() {
    return facetField().getAnnotation(Batched.class) != null;
  }

  default boolean isUsedToGroupBatches() {
    return facetField().getAnnotation(BatchesGroupedBy.class) != null;
  }

  @SneakyThrows
  default List<? extends AnnotationMirror> annotations() {
    return facetField().getAnnotationMirrors();
  }
}
