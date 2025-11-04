package com.flipkart.krystal.vajram.graphql.api;

import java.util.List;
import java.util.Map;

/**
 * Utility class for creating GraphQL-compliant responses.
 *
 * <p>This class provides a single utility method to create a complete GraphQL response with data
 * and errors. For accessing errors directly, use the methods in {@link AbstractGraphQlModel}:
 *
 * <ul>
 *   <li>{@code entity._graphqlErrorsAsList()} - Get errors as a spec-compliant flat list
 *   <li>{@code entity._graphqlErrors()} - Get errors as GraphQLFieldError objects
 *   <li>{@code entity._errors().isEmpty()} - Check if entity has errors
 * </ul>
 */
public final class GraphQLErrorUtils {

  private GraphQLErrorUtils() {
    // Utility class
  }

  /**
   * Creates a GraphQL-compliant response map with data and errors according to the specification.
   *
   * <p>This is the primary method for creating a complete GraphQL response. It automatically
   * collects errors from the entity and all nested entities, building proper paths including array
   * indices.
   *
   * <p>Response format follows the GraphQL spec
   * (https://spec.graphql.org/October2021/#sec-Response-Format):
   *
   * <pre>{@code
   * {
   *   "data": { ... },
   *   "errors": [  // Flat list with nested paths
   *     {
   *       "message": "Error description",
   *       "path": ["parent", "nested", 0, "field"],  // Supports array indices
   *       "extensions": {
   *         "code": "ERROR_CODE",
   *         "exception": "java.lang.RuntimeException"
   *       }
   *     }
   *   ]
   * }
   * }</pre>
   *
   * <p><b>Error Path Examples:</b>
   *
   * <ul>
   *   <li>Simple field: {@code ["name"]}
   *   <li>Nested object: {@code ["user", "address", "city"]}
   *   <li>Array item: {@code ["items", 0, "name"]}
   *   <li>Deep nesting: {@code ["order", "items", 2, "product", "price"]}
   * </ul>
   *
   * @param data The data entity (automatically collects errors from nested entities)
   * @return A map containing "data" and optionally "errors" (as a flat list per spec)
   */
  public static Map<String, Object> createGraphQLResponse(AbstractGraphQlModel<?> data) {
    Map<String, Object> response = new java.util.LinkedHashMap<>();
    response.put("data", data._valuesAsMap());

    // Per GraphQL spec: errors must be a list at the top level, not grouped by field
    // This automatically collects errors from nested entities with proper paths
    List<Map<String, Object>> errors = data._graphqlErrorsAsList();
    if (!errors.isEmpty()) {
      response.put("errors", errors);
    }

    return response;
  }
}
