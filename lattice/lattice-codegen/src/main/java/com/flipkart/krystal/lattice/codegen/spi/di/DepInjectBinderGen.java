package com.flipkart.krystal.lattice.codegen.spi.di;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.squareup.javapoet.CodeBlock;

public interface DepInjectBinderGen {
  CodeBlock getBinderCreationCode(LatticeCodegenContext latticeCodegenContext);

  boolean isApplicable(LatticeCodegenContext latticeCodegenContext);
}
