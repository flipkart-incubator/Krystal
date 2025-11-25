package com.flipkart.krystal.vajram.graphql.codegen;

import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
import com.google.auto.service.AutoService;
import java.util.Set;

@AutoService(ModelsCodeGeneratorProvider.class)
public class GraphQlRespJsonModelGenProvider implements ModelsCodeGeneratorProvider {
  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new GraphQlRespJsonModelGen(codeGenContext);
  }

  @Override
  public Set<Class<? extends ModelProtocol>> getSupportedModelProtocols() {
    return Set.of(GraphQlResponseJson.class);
  }
}
