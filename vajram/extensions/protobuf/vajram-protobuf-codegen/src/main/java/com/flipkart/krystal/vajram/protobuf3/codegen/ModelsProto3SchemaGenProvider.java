package com.flipkart.krystal.vajram.protobuf3.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.vajram.protobuf3.Protobuf3;
import com.google.auto.service.AutoService;
import java.util.Set;

@AutoService(ModelsCodeGeneratorProvider.class)
public class ModelsProto3SchemaGenProvider implements ModelsCodeGeneratorProvider {

  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new ModelsProto3SchemaGen(codeGenContext);
  }

  @Override
  public Set<Class<? extends ModelProtocol>> getSupportedModelProtocols() {
    return Set.of(Protobuf3.class);
  }
}
