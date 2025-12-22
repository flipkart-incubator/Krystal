package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.serial.SerdeProtocol;

public final class GraphQlInputJson implements SerdeProtocol {

  public static final GraphQlInputJson INSTANCE = new GraphQlInputJson();

  @Override
  public String modelClassesSuffix() {
    return "GQlInputJson";
  }

  @Override
  public String defaultContentType() {
    return "application/graphql-input+json";
  }

  private GraphQlInputJson() {}
}
