package com.flipkart.krystal.vajram.codegen;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

record TypeAndName(
    TypeName typeName, @Nullable TypeMirror type, List<AnnotationSpec> annotationSpecs) {
  TypeAndName(TypeName typeName) {
    this(typeName, null, List.of());
  }
}
