package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelProtocol;

public final class GraphQlResponse implements ModelProtocol {

  public static final GraphQlResponse INSTANCE = new GraphQlResponse();

  public static final String GRAPHQL_RESPONSE_JSON_CONTENT_TYPE =
      "application/graphql-response+json";

  @Override
  public String modelClassesSuffix() {
    return "GQlResp";
  }

  private GraphQlResponse() {}
}
