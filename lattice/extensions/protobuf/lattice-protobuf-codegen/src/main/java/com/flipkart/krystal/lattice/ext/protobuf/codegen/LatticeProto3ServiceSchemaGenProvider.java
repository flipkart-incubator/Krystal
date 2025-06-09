package com.flipkart.krystal.lattice.ext.protobuf.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.google.auto.service.AutoService;

@AutoService(LatticeCodeGeneratorProvider.class)
public class LatticeProto3ServiceSchemaGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new LatticeProto3ServiceSchemaGen(latticeCodegenContext);
  }
}
