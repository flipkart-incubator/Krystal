package com.flipkart.krystal.vajram.codegen.common.spi;

public interface VajramCodeGeneratorProvider {
  VajramCodeGenerator create(CodeGeneratorCreationContext creationContext);
}
