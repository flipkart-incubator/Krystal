package com.flipkart.krystal.lattice.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;

public interface LatticeCodeGeneratorProvider {
  CodeGenerator create(LatticeCodegenContext latticeCodegenContext);
}
