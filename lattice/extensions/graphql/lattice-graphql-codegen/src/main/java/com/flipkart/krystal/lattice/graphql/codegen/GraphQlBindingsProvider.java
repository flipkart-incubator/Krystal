package com.flipkart.krystal.lattice.graphql.codegen;

import static com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility.getRequestInterfaceName;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.flipkart.krystal.lattice.codegen.spi.di.ProviderMethod;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationDispatch;
import com.flipkart.krystal.vajram.graphql.codegen.GraphQLTypeName;
import com.flipkart.krystal.vajram.graphql.codegen.GraphQlCodeGenUtil;
import com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil;
import com.flipkart.krystal.vajramexecutor.krystex.VajramGraph;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoService(BindingsProvider.class)
public class GraphQlBindingsProvider implements BindingsProvider {

  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    GraphQlCodeGenUtil graphQlCodeGenUtil =
        new GraphQlCodeGenUtil(context.codeGenUtility().codegenUtil());
    SchemaReaderUtil schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
    Map<GraphQLTypeName, ObjectTypeDefinition> aggregatableTypes =
        schemaReaderUtil.aggregatableTypes();
    List<ClassName> operationTypes = new ArrayList<>();
    for (GraphQLTypeName graphQLTypeName : aggregatableTypes.keySet()) {
      if (schemaReaderUtil.isOperationType(graphQLTypeName)) {
        operationTypes.add(
            getRequestInterfaceName(schemaReaderUtil.getAggregatorName(graphQLTypeName)));
      }
    }

    return ImmutableList.of(
        new BindingsContainer(
            ImmutableList.of(
                new ProviderMethod(
                    GraphQlOperationDispatch.class.getSimpleName(),
                    ClassName.get(GraphQlOperationDispatch.class),
                    List.of(
                        ParameterSpec.builder(VajramGraph.class, "vajramGraph").build(),
                        ParameterSpec.builder(
                                TypeDefinitionRegistry.class, "typeDefinitionRegistry")
                            .build()),
                    CodeBlock.of(
                        "return new $T($L, $L, $T.of($L));",
                        GraphQlOperationDispatch.class,
                        "vajramGraph",
                        "typeDefinitionRegistry",
                        Set.class,
                        operationTypes.stream()
                            .map(className -> CodeBlock.of("$T.class", className))
                            .collect(CodeBlock.joining(", "))),
                    AnnotationSpec.builder(ApplicationScoped.class).build()))));
  }
}
