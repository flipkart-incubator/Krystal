package com.flipkart.krystal.vajram.protobuf3.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.VajramCodeGeneratorProvider;

@Deprecated(forRemoval = true)
// @AutoService(VajramCodeGeneratorProvider.class)
public class VajramModelsProto3SchemaGenProvider implements VajramCodeGeneratorProvider {

  @Override
  public CodeGenerator create(VajramCodeGenContext codeGenContext) {
    //noinspection removal
    return new VajramModelsProto3SchemaGen(codeGenContext);
  }
}
