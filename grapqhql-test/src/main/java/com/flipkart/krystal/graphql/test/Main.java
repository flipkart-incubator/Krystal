package com.flipkart.krystal.graphql.test;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.graphql.test.datafetchers.DataFetcherUtil;
import com.flipkart.krystal.graphql.test.productpage.ProductPageRequestContext;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import com.google.common.collect.ImmutableMap;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.File;
import java.nio.file.Files;

public class Main {

  public static final String APPLICATION_REQUEST_CONTEXT = "applicationRequestContext";

  public static void main(String[] args) {

    try {
      String schema =
          Files.readString(
              new File(
                      requireNonNull(Main.class.getClassLoader().getResource("schema.graphql"))
                          .toURI())
                  .toPath());
      SchemaParser schemaParser = new SchemaParser();
      TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

      RuntimeWiring runtimeWiring =
          newRuntimeWiring()
              .type("Query", builder -> builder.defaultDataFetcher(DataFetcherUtil::getField))
              .type("Product", builder -> builder.defaultDataFetcher(DataFetcherUtil::getField))
              .type("Listing", builder -> builder.defaultDataFetcher(DataFetcherUtil::getField))
              .build();

      SchemaGenerator schemaGenerator = new SchemaGenerator();
      GraphQLSchema graphQLSchema =
          schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

      GraphQL build = GraphQL.newGraphQL(graphQLSchema).build();
      ApplicationRequestContext applicationRequestContext =
          new ProductPageRequestContext("FSN_1", null, "request_id");
      ImmutableMap<Object, Object> graphQLContext;
      try (KrystexVajramExecutor<ApplicationRequestContext> vajramExecutor =
          new KrystexVajramExecutor<>(
              VajramNodeGraph.loadFromClasspath("com.flipkart.krystal.graphql"),
              applicationRequestContext)) {
        graphQLContext =
            ImmutableMap.of(
                APPLICATION_REQUEST_CONTEXT,
                applicationRequestContext,
                "vajram_executor",
                vajramExecutor);
        ExecutionInput executionInput =
            ExecutionInput.newExecutionInput()
                .graphQLContext(graphQLContext)
                .query(
                    """
                    {
                      primary_product{
                        product_id
                        title
                        sub_title
                        preferred_listing {
                          listing_id
                          price
                        }
                      }
                    }""")
                .build();
        ExecutionResult executionResult = build.execute(executionInput);
        executionResult.getErrors().stream()
            .findAny()
            .ifPresent(graphQLError -> System.err.println(executionResult.getErrors()));
        System.out.println(executionResult.getData().toString());
      }
    } catch (Exception e) {
      System.err.println(e);
      throw new RuntimeException(e);
    }
  }
}
