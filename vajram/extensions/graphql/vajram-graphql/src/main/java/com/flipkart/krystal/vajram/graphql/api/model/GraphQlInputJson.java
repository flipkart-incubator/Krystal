package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelProtocol;

/**
 * Model protocol for GraphQL input types.
 *
 * <p>This protocol is used to generate GraphQL input model classes with the "GQlInputJson" suffix.
 * GraphQL input types are used for type coercion from GraphQL variables (JSON maps) to typed Java
 * classes, and do not require serialization/deserialization protocols.
 *
 * <p>This maintains a separate protocol class to preserve the "GQlInputJson" suffix in generated
 * class names for backward compatibility.
 */
public final class GraphQlInputJson implements ModelProtocol {

  public static final GraphQlInputJson INSTANCE = new GraphQlInputJson();

  @Override
  public String modelClassesSuffix() {
    return "GQlInputJson";
  }

  private GraphQlInputJson() {}
}
