package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.serial.SerdeProtocol;

public final class GraphQlResponseJson implements SerdeProtocol {

  public static final GraphQlResponseJson INSTANCE = new GraphQlResponseJson();

  @Override
  public String modelClassesSuffix() {
    return "GraphQl";
  }

  @Override
  public String defaultContentType() {
    return "application/graphql-response+json";
  }

  private GraphQlResponseJson() {}
}
