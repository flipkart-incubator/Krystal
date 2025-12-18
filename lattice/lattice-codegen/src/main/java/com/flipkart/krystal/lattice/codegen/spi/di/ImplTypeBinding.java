package com.flipkart.krystal.lattice.codegen.spi.di;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;

public record ImplTypeBinding(
    ClassName parentType, ClassName childType, AnnotationSpec scope, boolean isSoleImpl)
    implements Binding {

  public ImplTypeBinding(ClassName parentType, ClassName childType, AnnotationSpec scope) {
    this(parentType, childType, scope, false);
  }

  @Override
  public String identifierName() {
    return parentType.simpleName() + "_" + childType.simpleName();
  }
}
