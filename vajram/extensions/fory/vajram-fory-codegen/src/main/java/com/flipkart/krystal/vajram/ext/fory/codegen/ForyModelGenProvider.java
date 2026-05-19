package com.flipkart.krystal.vajram.ext.fory.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.vajram.fory.Fory;
import com.google.auto.service.AutoService;
import java.util.Set;

@AutoService(ModelsCodeGeneratorProvider.class)
public class ForyModelGenProvider implements ModelsCodeGeneratorProvider {

  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new ForyModelsGen(codeGenContext);
  }

  @Override
  public Set<Class<? extends ModelProtocol>> getSupportedModelProtocols() {
    return Set.of(Fory.class);
  }
}
