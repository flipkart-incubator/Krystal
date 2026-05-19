package com.flipkart.krystal.vajram.graphql.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlInputJson;
import com.google.auto.service.AutoService;
import java.util.Set;

@AutoService(ModelsCodeGeneratorProvider.class)
public class GraphQlInputJsonModelGenProvider implements ModelsCodeGeneratorProvider {
  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new GraphQlInputJsonModelGen(codeGenContext);
  }

  @Override
  public Set<Class<? extends ModelProtocol>> getSupportedModelProtocols() {
    return Set.of(GraphQlInputJson.class);
  }
}
