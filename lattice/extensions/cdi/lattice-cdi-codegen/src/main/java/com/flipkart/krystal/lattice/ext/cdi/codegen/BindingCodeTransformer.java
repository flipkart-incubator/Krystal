package com.flipkart.krystal.lattice.ext.cdi.codegen;

import com.palantir.javapoet.MethodSpec;

public interface BindingCodeTransformer {
  default void transform(MethodSpec.Builder methodSpecBuilder) {}
}
