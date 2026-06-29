package com.flipkart.krystal.vajram.graphql.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponse;
import com.google.auto.service.AutoService;
import java.util.Set;

@AutoService(ModelsCodeGeneratorProvider.class)
public class GraphQlRespModelGenProvider implements ModelsCodeGeneratorProvider {
  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new GraphQlRespModelGen(codeGenContext);
  }

  @Override
  public Set<Class<? extends ModelProtocol>> getSupportedModelProtocols() {
    return Set.of(GraphQlResponse.class);
  }
}
