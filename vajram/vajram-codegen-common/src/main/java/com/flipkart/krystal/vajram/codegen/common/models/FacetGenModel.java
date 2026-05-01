package com.flipkart.krystal.vajram.codegen.common.models;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface FacetGenModel permits DefaultFacetModel, DependencyModel {
  int id();

  String name();

  VajramInfoLite vajramInfo();

  Element facetElement();

  FacetType facetType();

  CodeGenType dataType();

  default boolean isGiven() {
    return false;
  }

  default boolean isMandatoryOnServer() {
    IfAbsent ifAbsent = facetElement().getAnnotation(IfAbsent.class);
    return ifAbsent != null && ifAbsent.value().isMandatoryOnServer();
  }

  default boolean isOptionalForClient() {
    IfAbsent ifAbsent = facetElement().getAnnotation(IfAbsent.class);
    return ifAbsent == null || ifAbsent.value().isOptionalForClient();
  }

  @Nullable String documentation();

  default boolean isBatched() {
    return facetElement().getAnnotation(Batched.class) != null;
  }

  default boolean isUsedToGroupBatches() {
    return facetElement().getAnnotation(BatchesGroupedBy.class) != null;
  }

  @SneakyThrows
  default List<? extends AnnotationMirror> annotations() {
    return facetElement().getAnnotationMirrors();
  }
}
