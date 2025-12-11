package com.flipkart.krystal.lattice.codegen.spi.di;

import com.squareup.javapoet.ClassName;

public record ImplTypeBinding(
    ClassName parentType, ClassName childType, BindingScope bindingScope, boolean isSoleImpl)
    implements Binding {

  public ImplTypeBinding(ClassName parentType, ClassName childType, BindingScope bindingScope) {
    this(parentType, childType, bindingScope, false);
  }

  @Override
  public String identifierName() {
    return parentType.simpleName() + "_" + childType.simpleName();
  }
}
