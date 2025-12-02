package com.flipkart.krystal.vajram.codegen.common.generators;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.PlainJavaObject;
import com.google.auto.service.AutoService;
import java.util.Set;

@AutoService(ModelsCodeGeneratorProvider.class)
public class JavaModelGeneratorProvider implements ModelsCodeGeneratorProvider {

  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new JavaModelsGenerator(codeGenContext);
  }

  @Override
  public Set<Class<? extends ModelProtocol>> getSupportedModelProtocols() {
    return Set.of(PlainJavaObject.class);
  }
}
