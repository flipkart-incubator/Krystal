package com.flipkart.krystal.lattice.ext.cdi.codegen;

import com.squareup.javapoet.MethodSpec;

public interface BindingCodeTransformer {
  default void transform(MethodSpec.Builder methodSpecBuilder) {}
}
