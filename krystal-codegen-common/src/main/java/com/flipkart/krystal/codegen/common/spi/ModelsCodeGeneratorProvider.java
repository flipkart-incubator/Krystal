package com.flipkart.krystal.codegen.common.spi;

import com.flipkart.krystal.model.ModelProtocol;
import java.util.Set;

public interface ModelsCodeGeneratorProvider {
  CodeGenerator create(ModelsCodeGenContext codeGenContext);

  Set<Class<? extends ModelProtocol>> getSupportedModelProtocols();
}
