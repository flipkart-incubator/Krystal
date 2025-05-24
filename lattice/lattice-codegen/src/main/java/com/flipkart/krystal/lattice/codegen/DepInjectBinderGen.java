package com.flipkart.krystal.lattice.codegen;

import com.flipkart.krystal.lattice.core.di.DependencyInjectionBinder;
import com.squareup.javapoet.CodeBlock;

public interface DepInjectBinderGen {
  CodeBlock getBinderCreationCode(LatticeCodegenContext latticeCodegenContext);

  boolean isApplicable(LatticeCodegenContext latticeCodegenContext);

  CodeBlock getRequestScope();
}
