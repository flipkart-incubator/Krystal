package com.flipkart.krystal.vajram.codegen;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

record TypeAndName(
    TypeName typeName, Optional<TypeMirror> type, List<AnnotationSpec> annotationSpecs) {
  TypeAndName(TypeName typeName) {
    this(typeName, Optional.empty(), List.of());
  }
}
