package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.batching.Batched;
import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
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
  public default List<Annotation> getAnnotations() {
    List<Annotation> annotations = new ArrayList<>();
    for (AnnotationMirror annotationMirror : facetField().getAnnotationMirrors()) {
      TypeElement element = (TypeElement) annotationMirror.getAnnotationType().asElement();
      @SuppressWarnings("unchecked")
      Class<? extends Annotation> annotationType =
          (Class<? extends Annotation>) Class.forName(element.toString());
      Annotation annotation = facetField().getAnnotation(annotationType);
      if (annotation != null) {
        annotations.add(annotation);
      }
    }
    return annotations;
  }
}
