package com.flipkart.krystal.vajram.ext.json.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.vajram.json.Json;
import com.google.auto.service.AutoService;
import java.util.Set;

@AutoService(ModelsCodeGeneratorProvider.class)
public class JsonModelGenProvider implements ModelsCodeGeneratorProvider {

  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new JsonModelsGen(codeGenContext);
  }

  @Override
  public Set<Class<? extends ModelProtocol>> getSupportedModelProtocols() {
    return Set.of(Json.class);
  }
}
