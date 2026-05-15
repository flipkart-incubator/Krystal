package com.flipkart.krystal.codegen.common.models;

import com.squareup.javapoet.TypeName;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

public record TypeAndName(TypeName typeName, @Nullable TypeMirror type) {

  public TypeAndName(TypeMirror type) {
    this(new TypeNameVisitor(false).visit(type), type);
  }

  public TypeAndName(TypeName typeName) {
    this(typeName, null);
  }
}
