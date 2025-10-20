package com.flipkart.krystal.vajram.codegen.common.spi;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;

public interface VajramCodeGeneratorProvider {
  CodeGenerator create(VajramCodeGenContext codeGenContext);
}
