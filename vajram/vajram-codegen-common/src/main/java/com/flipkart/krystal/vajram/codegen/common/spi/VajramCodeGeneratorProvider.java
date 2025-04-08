package com.flipkart.krystal.vajram.codegen.common.spi;

public interface VajramCodeGeneratorProvider {
  CodeGenerator create(VajramCodeGenContext creationContext);
}
