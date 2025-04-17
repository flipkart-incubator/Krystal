package com.flipkart.krystal.vajram.codegen.common.spi;

public interface AllVajramsCodeGeneratorProvider {
  CodeGenerator create(AllVajramCodeGenContext codeGenContext);
}
