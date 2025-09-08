package com.flipkart.krystal.codegen.common.spi;

public interface ModelsCodeGeneratorProvider {
  CodeGenerator create(ModelsCodeGenContext codeGenContext);
}
