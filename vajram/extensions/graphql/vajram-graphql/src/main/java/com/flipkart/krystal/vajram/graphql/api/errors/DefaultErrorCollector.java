package com.flipkart.krystal.vajram.graphql.api.errors;

import static com.flipkart.krystal.vajram.graphql.api.execution.GraphQLUtils.convertToGraphQlError;

import graphql.GraphQLError;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link ErrorCollector} that accumulates errors in a list.
 *
 * <p>This implementation stores {@link GraphQLErrorInfo} instances internally and converts them to
 * {@link GraphQLError} instances when {@link #getErrors()} is called. This provides a clean
 * abstraction layer between error collection and the GraphQL-specific error format.
 */
final class DefaultErrorCollector implements ErrorCollector {

  private final List<GraphQLError> errorInfos = new ArrayList<>();

  @Override
  public void addError(GraphQLErrorInfo errorInfo) {
    errorInfos.add(convertToGraphQlError(errorInfo));
  }

  @Override
  public void addError(GraphQLError graphQLError) {
    errorInfos.add(graphQLError);
  }

  @Override
  public List<GraphQLError> getErrors() {
    // Convert all GraphQLErrorInfo instances to GraphQLError using GraphQLUtils
    return errorInfos;
  }
}
