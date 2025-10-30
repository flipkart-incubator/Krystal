package com.flipkart.krystal.vajram.graphql.codegen;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGeneratorProvider;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.google.auto.service.AutoService;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;

@AutoService(ModelsCodeGeneratorProvider.class)
public class GraphQlTypeModelGenProvider implements ModelsCodeGeneratorProvider {
  @Override
  public CodeGenerator create(ModelsCodeGenContext codeGenContext) {
    return new GraphQlTypeModelGen(codeGenContext);
  }
}
