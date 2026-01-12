package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.serial.SerdeProtocol;

/**
 * Serde protocol for GraphQL input types.
 *
 * <p>According to the GraphQL over HTTP specification
 * (https://graphql.github.io/graphql-over-http/draft/), input variables must use "application/json"
 * content type, not a custom content type. This protocol maintains a separate class for backward
 * compatibility with generated code that uses the "GQlInputJson" suffix, but uses the standard
 * "application/json" content type as required by the spec.
 *
 * <p>Note: While this could theoretically use {@link com.flipkart.krystal.vajram.json.Json}
 * directly, we maintain this separate protocol class to preserve the "GQlInputJson" suffix in
 * generated class names for backward compatibility.
 */
public final class GraphQlInputJson implements SerdeProtocol {

  public static final GraphQlInputJson INSTANCE = new GraphQlInputJson();

  @Override
  public String modelClassesSuffix() {
    return "GQlInputJson";
  }

  @Override
  public String defaultContentType() {
    return "application/json";
  }

  private GraphQlInputJson() {}
}
