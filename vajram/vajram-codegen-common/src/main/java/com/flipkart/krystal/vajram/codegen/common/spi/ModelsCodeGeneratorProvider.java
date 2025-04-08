package com.flipkart.krystal.vajram.codegen.common.spi;

public interface ModelsCodeGeneratorProvider {
  CodeGenerator create(ModelsCodeGenContext codeGenContext);
}
