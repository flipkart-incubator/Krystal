package com.flipkart.krystal.lattice.codegen.spi;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;

public interface LatticeCodeGeneratorProvider {
  CodeGenerator create(LatticeCodegenContext latticeCodegenContext);
}
