package com.flipkart.krystal.vajram.graphql.api.schema;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import graphql.GraphQL;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.scalars.ExtendedScalars;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

@Singleton
public final class GraphQlLoader {

  private final ConcurrentHashMap<String, PreparsedDocumentEntry> documentCache =
      new ConcurrentHashMap<>();

  @Inject
  public GraphQlLoader() {}

  public GraphQL loadGraphQl() throws IOException {

    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistryComplete = new TypeDefinitionRegistry();

    for (Map.Entry<String, String> entry : getResourceFileContents().entrySet()) {
      typeDefinitionRegistryComplete.merge(schemaParser.parse(entry.getValue()));
    }

    RuntimeWiring.Builder runtimeWiring = RuntimeWiring.newRuntimeWiring();
    runtimeWiring.scalar(ExtendedScalars.Object);
    runtimeWiring.scalar(ExtendedScalars.DateTime);
    runtimeWiring.scalar(ExtendedScalars.Date);
    runtimeWiring.scalar(ExtendedScalars.GraphQLLong);

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    GraphQLSchema graphQLSchema =
        schemaGenerator.makeExecutableSchema(typeDefinitionRegistryComplete, runtimeWiring.build());

    PreparsedDocumentProvider preParsedCache =
        (executionInput, computeFunction) -> {
          Function<String, PreparsedDocumentEntry> mapCompute =
              key -> computeFunction.apply(executionInput);
          return completedFuture(
              documentCache.computeIfAbsent(executionInput.getQuery(), mapCompute));
        };

    return GraphQL.newGraphQL(graphQLSchema)
        .queryExecutionStrategy(new VajramExecutionStrategy())
        .preparsedDocumentProvider(preParsedCache)
        .build();
  }

  Map<String, String> getResourceFileContents() throws IOException {
    Map<String, String> fileToContentMap = new HashMap<>();
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.addUrls(ClasspathHelper.forResource("", this.getClass().getClassLoader()));
    builder.addScanners(Scanners.Resources);
    Reflections reflections = new Reflections(builder);
    Set<String> files = reflections.getResources(Pattern.compile("(.*).graphqls"));

    for (String file : files) {
      String content = this.readResourceFile(file);
      if (content != null) {
        fileToContentMap.put(file, content);
      }
    }

    return fileToContentMap;
  }

  private String readResourceFile(String filePath) throws IOException {
    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filePath);
    if (inputStream == null) {
      return null;
    } else {
      StringBuilder sb = new StringBuilder();
      BufferedReader br =
          new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

      for (int c = br.read(); c != -1; c = br.read()) {
        sb.append((char) c);
      }

      return sb.toString();
    }
  }
}
