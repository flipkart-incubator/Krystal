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
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

@Slf4j
@Singleton
public final class GraphQlLoader {

  private final ConcurrentHashMap<String, PreparsedDocumentEntry> documentCache =
      new ConcurrentHashMap<>();

  @Inject
  public GraphQlLoader() {}

  public GraphQL loadGraphQl(TypeDefinitionRegistry typeDefinitionRegistry) {
    // Extract root package name from schema
    String rootPackageName = extractRootPackageName(typeDefinitionRegistry);

    RuntimeWiring.Builder runtimeWiring = RuntimeWiring.newRuntimeWiring();
    runtimeWiring.scalar(ExtendedScalars.Object);
    runtimeWiring.scalar(ExtendedScalars.DateTime);
    runtimeWiring.scalar(ExtendedScalars.Date);
    runtimeWiring.scalar(ExtendedScalars.GraphQLLong);

    // Note: Input type coercion is handled at execution time via GraphQlInputTypeWiringFactory
    // The factory is available for use in execution context if needed

    GraphQLSchema graphQLSchema =
        new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring.build());

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

  public TypeDefinitionRegistry getTypeDefinitionRegistry() {
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistryComplete = new TypeDefinitionRegistry();
    for (Entry<String, InputStream> entry : getResourceFileContents().entrySet()) {
      typeDefinitionRegistryComplete.merge(schemaParser.parse(entry.getValue()));
    }
    return typeDefinitionRegistryComplete;
  }

  public Map<String, InputStream> getResourceFileContents() {
    Map<String, InputStream> fileToContentMap = new HashMap<>();
    ConfigurationBuilder builder = new ConfigurationBuilder();
    Collection<URL> resource = ClasspathHelper.forResource("Schema.graphqls");
    builder.addUrls(resource);
    builder.addScanners(Scanners.Resources);
    Reflections reflections = new Reflections(builder);
    Set<String> files = reflections.getResources(Pattern.compile("(.*)\\.graphqls"));

    for (String file : files) {
      InputStream content = null;
      for (ClassLoader classLoader : ClasspathHelper.classLoaders()) {
        content = classLoader.getResourceAsStream(file);
        if (content != null) {
          break;
        }
      }
      if (content != null) {
        fileToContentMap.put(file, content);
      } else {
        log.error("Could not read GraphQL schema file {}", file);
      }
    }
    log.info("GraphQl Files loaded: {}", fileToContentMap.keySet());
    return fileToContentMap;
  }

  /**
   * Extracts the root package name from the GraphQL schema definition.
   *
   * <p>The root package is specified using the {@code @rootPackage(name: "...")} directive on the
   * schema.
   *
   * @param typeDefinitionRegistry The GraphQL schema type registry
   * @return The root package name, or null if not found
   */
  @Nullable
  private String extractRootPackageName(TypeDefinitionRegistry typeDefinitionRegistry) {
    return typeDefinitionRegistry
        .schemaDefinition()
        .flatMap(
            schemaDef ->
                schemaDef.getDirectives().stream()
                    .filter(
                        directive ->
                            directive.getName().equals("rootPackage")
                                || directive.getName().equalsIgnoreCase("rootpackage"))
                    .findFirst()
                    .flatMap(
                        directive ->
                            directive.getArguments().stream()
                                .filter(arg -> arg.getName().equals("name"))
                                .findFirst()
                                .map(
                                    arg ->
                                        arg.getValue() instanceof graphql.language.StringValue
                                            ? ((graphql.language.StringValue) arg.getValue())
                                                .getValue()
                                            : null)))
        .orElse(null);
  }
}
