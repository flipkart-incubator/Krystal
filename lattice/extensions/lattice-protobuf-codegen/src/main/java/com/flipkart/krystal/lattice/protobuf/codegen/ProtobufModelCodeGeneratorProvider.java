package com.flipkart.krystal.lattice.protobuf.codegen;

import com.flipkart.krystal.vajram.codegen.common.spi.CodeGeneratorCreationContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;
import com.google.auto.service.AutoService;

@AutoService(VajramCodeGeneratorProvider.class)
public class ProtobufModelCodeGeneratorProvider implements VajramCodeGeneratorProvider {

  @Override
  public VajramCodeGenerator create(CodeGeneratorCreationContext creationContext) {
    return new ProtobufModelCodeGenerator(creationContext);
  }
}
