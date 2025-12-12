package com.flipkart.krystal.vajram.graphql.api.schema;

import static com.flipkart.krystal.vajram.graphql.api.model.GraphQlInputJson.INSTANCE;

import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Registry for mapping GraphQL input type names to their corresponding Java classes.
 *
 * <p>This class maintains a cache of GraphQL input type names to generated Krystal input model
 * classes, enabling variable coercion at the execution level.
 */
@Slf4j
public class GraphQlInputTypeWiringFactory {

  private final TypeDefinitionRegistry typeDefinitionRegistry;
  private final String rootPackageName;
  private final Map<String, Class<?>> inputTypeClassCache = new ConcurrentHashMap<>();

  /**
   * Creates a registry for the given schema.
   *
   * @param typeDefinitionRegistry The GraphQL schema type registry
   * @param rootPackageName The root package name where input types are generated (e.g.,
   *     "com.flipkart.krystal.vajram.graphql.samples")
   */
  public GraphQlInputTypeWiringFactory(
      TypeDefinitionRegistry typeDefinitionRegistry, String rootPackageName) {
    this.typeDefinitionRegistry = typeDefinitionRegistry;
    this.rootPackageName = rootPackageName;
  }

  /**
   * Gets the Java class for a GraphQL input type name.
   *
   * @param graphQlTypeName The GraphQL input type name (e.g., "SellerInput")
   * @return The Java class, or null if not found
   */
  @Nullable
  public Class<?> getInputTypeClass(String graphQlTypeName) {
    return findInputTypeClass(graphQlTypeName);
  }

  /**
   * Gets the coercing instance for a GraphQL input type name.
   *
   * @param graphQlTypeName The GraphQL input type name (e.g., "SellerInput")
   * @return The coercing instance, or null if the class is not found
   */
  @Nullable
  public GraphQlInputTypeCoercing getCoercing(String graphQlTypeName) {
    Class<?> inputTypeClass = findInputTypeClass(graphQlTypeName);
    if (inputTypeClass == null) {
      return null;
    }
    return new GraphQlInputTypeCoercing(graphQlTypeName, inputTypeClass);
  }

  /**
   * Attempts to find the generated input type class for a given GraphQL type name.
   *
   * <p>For example, for GraphQL type "SellerInput", this will look for {@code
   * SellerInput_ImmutGQlInputJson} in the input package.
   */
  @Nullable
  private Class<?> findInputTypeClass(String graphQlTypeName) {
    return inputTypeClassCache.computeIfAbsent(
        graphQlTypeName,
        typeName -> {
          try {
            String packageName = rootPackageName + ".input";
            String className = typeName + INSTANCE.modelClassesSuffix();
            String fullClassName = packageName + "." + className;

            try {
              return Class.forName(fullClassName);
            } catch (ClassNotFoundException e) {
              log.debug(
                  "Could not find input type class: {}. Input types may not be generated yet.",
                  fullClassName);
              return null;
            }
          } catch (Exception e) {
            log.warn("Error finding input type class for {}: {}", typeName, e.getMessage());
            return null;
          }
        });
  }
}
