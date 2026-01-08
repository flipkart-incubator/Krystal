package com.flipkart.krystal.vajram.graphql.api.schema;

import graphql.schema.idl.TypeDefinitionRegistry;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract base class for mapping GraphQL input type names to their corresponding Java classes.
 *
 * <p>This class follows Krystal's "No reflection" principle. A concrete implementation is generated
 * at compile time by {@code GraphQlInputTypeRegistryGen} that directly imports and calls the
 * registry's static method, eliminating all reflection usage.
 *
 * <p>The generated implementation ({@code GraphQlInputTypeWiringFactoryImpl}) is located in the
 * root package and should be used directly, or instantiated via the static factory method.
 */
public abstract class GraphQlInputTypeWiringFactory {

  /**
   * Gets the Java class for a GraphQL input type name.
   *
   * @param graphQlTypeName The GraphQL input type name (e.g., "SellerInput")
   * @return The Java class, or null if not found
   */
  @Nullable
  public abstract Class<?> getInputTypeClass(String graphQlTypeName);

  /**
   * Gets the coercing instance for a GraphQL input type name.
   *
   * @param graphQlTypeName The GraphQL input type name (e.g., "SellerInput")
   * @return The coercing instance, or null if the class is not found
   */
  @Nullable
  public GraphQlInputTypeCoercing getCoercing(String graphQlTypeName) {
    Class<?> inputTypeClass = getInputTypeClass(graphQlTypeName);
    if (inputTypeClass == null) {
      return null;
    }
    return new GraphQlInputTypeCoercing(graphQlTypeName, inputTypeClass);
  }

  /**
   * Creates a factory instance by loading the generated implementation.
   *
   * <p>This method uses Class.forName once to load the generated factory implementation class. The
   * generated implementation itself uses zero reflection - it directly imports and calls the
   * registry's static method.
   *
   * @param typeDefinitionRegistry The GraphQL schema type registry
   * @param rootPackageName The root package name where the generated factory is located
   * @return A factory instance, or null if the generated factory class cannot be loaded
   */
  @Nullable
  public static GraphQlInputTypeWiringFactory create(
      TypeDefinitionRegistry typeDefinitionRegistry, String rootPackageName) {
    try {
      String factoryClassName = rootPackageName + ".GraphQlInputTypeWiringFactoryImpl";
      Class<?> factoryClass = Class.forName(factoryClassName);
      java.lang.reflect.Constructor<?> constructor =
          factoryClass.getDeclaredConstructor(TypeDefinitionRegistry.class);
      return (GraphQlInputTypeWiringFactory) constructor.newInstance(typeDefinitionRegistry);
    } catch (Exception e) {
      // Generated factory may not exist yet, or no input types exist
      return null;
    }
  }
}
