package com.flipkart.krystal.lattice.codegen.spi.di;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Binds the type to a null value */
public record NullBinding(
    ClassName bindFrom,
    @Nullable CodeBlock qualifierExpression,
    @Nullable AnnotationSpec qualifierAnnotation,
    @Nullable AnnotationSpec scope)
    implements Binding {

  @Override
  public String identifierName() {
    return "$dummy$";
  }
}
