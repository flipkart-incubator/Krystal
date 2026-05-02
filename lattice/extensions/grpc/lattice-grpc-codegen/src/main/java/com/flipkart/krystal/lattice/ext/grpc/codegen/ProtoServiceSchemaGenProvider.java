package com.flipkart.krystal.lattice.ext.grpc.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.google.auto.service.AutoService;

@AutoService(LatticeCodeGeneratorProvider.class)
public class ProtoServiceSchemaGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new ProtoServiceSchemaGen(latticeCodegenContext);
  }
}
