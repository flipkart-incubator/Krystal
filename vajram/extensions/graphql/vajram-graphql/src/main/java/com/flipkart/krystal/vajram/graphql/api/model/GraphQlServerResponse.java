package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.ModelProtocol;

public final class GraphQlServerResponse implements ModelProtocol {

  public static final GraphQlServerResponse INSTANCE = new GraphQlServerResponse();

  public static final String GRAPHQL_RESPONSE_JSON_CONTENT_TYPE =
      "application/graphql-response+json";

  @Override
  public String modelClassesSuffix() {
    return "GQlServerResp";
  }

  private GraphQlServerResponse() {}
}
