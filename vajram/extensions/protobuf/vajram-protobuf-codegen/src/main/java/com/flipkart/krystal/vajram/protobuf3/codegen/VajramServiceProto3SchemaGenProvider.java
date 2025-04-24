package com.flipkart.krystal.vajram.protobuf3.codegen;

import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramCodeGenContext;
import com.flipkart.krystal.vajram.codegen.common.spi.AllVajramsCodeGeneratorProvider;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.google.auto.service.AutoService;

@AutoService(AllVajramsCodeGeneratorProvider.class)
public class VajramServiceProto3SchemaGenProvider implements AllVajramsCodeGeneratorProvider {

  @Override
  public CodeGenerator create(AllVajramCodeGenContext codeGenContext) {
    return new VajramServiceProto3SchemaGen(codeGenContext);
  }
}
