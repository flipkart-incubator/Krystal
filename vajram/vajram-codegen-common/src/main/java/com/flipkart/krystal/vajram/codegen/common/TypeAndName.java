package com.flipkart.krystal.vajram.codegen.common;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

public record TypeAndName(
    TypeName typeName, @Nullable TypeMirror type, List<AnnotationSpec> annotationSpecs) {
  TypeAndName(TypeName typeName) {
    this(typeName, null, List.of());
  }
}
