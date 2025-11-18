package com.flipkart.krystal.vajram.graphql.api;

import graphql.GraphQLError;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link ErrorCollector} that accumulates errors in a list.
 *
 * <p>This implementation stores {@link GraphQLErrorInfo} instances internally and converts them to
 * {@link GraphQLError} instances when {@link #getErrors()} is called. This provides a clean
 * abstraction layer between error collection and the GraphQL-specific error format.
 */
public class DefaultErrorCollector implements ErrorCollector {

  private final List<GraphQLErrorInfo> errorInfos = new ArrayList<>();

  @Override
  public void addError(GraphQLErrorInfo errorInfo) {
    errorInfos.add(errorInfo);
  }

  @Override
  public List<GraphQLError> getErrors() {
    // Convert all GraphQLErrorInfo instances to GraphQLError using GraphQLUtils
    return errorInfos.stream()
        .map(GraphQLUtils::convertToGraphQlError)
        .collect(Collectors.toList());
  }
}
