package com.flipkart.krystal.lattice.codegen;

import com.squareup.javapoet.CodeBlock;

public interface DepInjectBinderGen {
  CodeBlock getBinderCreationCode(LatticeCodegenContext latticeCodegenContext);

  boolean isApplicable(LatticeCodegenContext latticeCodegenContext);

  CodeBlock getRequestScope();
}
