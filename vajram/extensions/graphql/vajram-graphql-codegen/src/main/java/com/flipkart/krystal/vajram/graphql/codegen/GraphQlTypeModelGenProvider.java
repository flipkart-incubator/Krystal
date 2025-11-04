package com.flipkart.krystal.vajram.graphql.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.google.auto.service.AutoService;

@AutoService(ModelsCodeGeneratorProvider.class)
public class GraphQlTypeModelGenProvider implements ModelsCodeGeneratorProvider {
  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new GraphQlTypeModelGen(codeGenContext);
  }
}
