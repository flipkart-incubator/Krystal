package com.flipkart.krystal.vajram.graphql.api.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlInputJson;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom GraphQL input type coercing that deserializes JSON maps to generated Krystal input model
 * classes (e.g., {@code SellerInput_ImmutGQlInputJson}).
 *
 * <p>This coercing handles the conversion of GraphQL variables (which come as JSON maps) to the
 * generated immutable input type classes that support {@link GraphQlInputJson} serialization.
 *
 * <p>Each instance is bound to a specific GraphQL input type name and Java class.
 */
@Slf4j
public class GraphQlInputTypeCoercing implements Coercing<Object, Object> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final String graphQlTypeName;
  private final Class<?> inputTypeClass;

  /**
   * Creates a coercing for a specific GraphQL input type.
   *
   * @param graphQlTypeName The GraphQL input type name (e.g., "SellerInput")
   * @param inputTypeClass The Java class to deserialize to (e.g., SellerInput_ImmutGQlInputJson)
   */
  public GraphQlInputTypeCoercing(String graphQlTypeName, Class<?> inputTypeClass) {
    this.graphQlTypeName = graphQlTypeName;
    this.inputTypeClass = inputTypeClass;
  }

  @Override
  public Object serialize(Object dataFetcherResult) throws CoercingSerializeException {
    // For serialization (output), we typically don't serialize input types
    // But if needed, we can serialize the model to JSON
    if (dataFetcherResult == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.convertValue(dataFetcherResult, Map.class);
    } catch (Exception e) {
      throw new CoercingSerializeException(
          "Failed to serialize input type: " + dataFetcherResult.getClass().getName(), e);
    }
  }

  @Override
  public Object parseValue(Object input) throws CoercingParseValueException {
    if (input == null) {
      return null;
    }

    // If input is already the correct type, return it
    if (inputTypeClass.isInstance(input)) {
      return input;
    }

    // If input is a Map, deserialize it to the input type class
    if (input instanceof Map<?, ?> map) {
      try {
        return OBJECT_MAPPER.convertValue(map, inputTypeClass);
      } catch (Exception e) {
        throw new CoercingParseValueException(
            "Failed to deserialize map to " + inputTypeClass.getName() + ": " + e.getMessage(), e);
      }
    }

    throw new CoercingParseValueException(
        "Cannot parse value of type " + input.getClass() + " as " + graphQlTypeName);
  }

  @Override
  public Object parseLiteral(Object input) throws CoercingParseLiteralException {
    // GraphQL literals are typically used in queries, not variables
    // Variables are handled by parseValue
    if (input == null) {
      return null;
    }

    // Convert GraphQL literal to a Map and then deserialize
    if (input instanceof Map<?, ?> map) {
      try {
        return OBJECT_MAPPER.convertValue(map, inputTypeClass);
      } catch (Exception e) {
        throw new CoercingParseLiteralException(
            "Failed to deserialize literal to " + inputTypeClass.getName() + ": " + e.getMessage(),
            e);
      }
    }

    throw new CoercingParseLiteralException(
        "Cannot parse literal of type " + input.getClass() + " as " + graphQlTypeName);
  }

  /**
   * Static helper method to coerce an input type argument from GraphQL. This is used by generated
   * code to convert Map arguments to input type instances.
   *
   * @param argumentValue The raw argument value from GraphQL (typically a Map)
   * @param inputTypeClass The Java class to deserialize to (e.g., SellerInput_ImmutGQlInputJson)
   * @return The coerced input type instance, or null if argumentValue is null
   */
  @SuppressWarnings("unchecked")
  public static Object coerceInputType(Object argumentValue, Class<?> inputTypeClass) {
    if (argumentValue == null) {
      return null;
    }

    // If already the correct type, return it
    if (inputTypeClass.isInstance(argumentValue)) {
      return argumentValue;
    }

    // If it's a Map, deserialize it
    if (argumentValue instanceof Map<?, ?> map) {
      try {
        return OBJECT_MAPPER.convertValue(map, inputTypeClass);
      } catch (Exception e) {
        throw new RuntimeException(
            "Failed to coerce input type " + inputTypeClass.getName() + ": " + e.getMessage(), e);
      }
    }

    return argumentValue;
  }
}
