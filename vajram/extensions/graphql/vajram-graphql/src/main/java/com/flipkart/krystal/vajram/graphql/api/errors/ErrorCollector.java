package com.flipkart.krystal.vajram.graphql.api.errors;

import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLUtils;
import graphql.GraphQLError;
import java.util.List;

/**
 * Collector interface for accumulating GraphQL errors during response traversal.
 *
 * <p>This interface uses the Visitor pattern to traverse GraphQL response models and collect errors
 * with their proper paths. It works with {@link GraphQLErrorInfo} abstraction to decouple error
 * collection from the GraphQL-specific error format.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * ErrorCollector collector = new DefaultErrorCollector();
 * model._collectErrors(collector, new ArrayList<>());
 * List<GraphQLError> errors = collector.getErrors();
 * }</pre>
 */
public interface ErrorCollector {

  static ErrorCollector defaultCollector() {
    return new DefaultErrorCollector();
  }

  /**
   * Adds a GraphQL error info to the collection.
   *
   * <p>The error info will be converted to a GraphQLError when {@link #getErrors()} is called.
   *
   * @param errorInfo The error information to add
   */
  void addError(GraphQLErrorInfo errorInfo);

  /**
   * Returns all collected errors as GraphQLError instances.
   *
   * <p>This method converts all accumulated {@link GraphQLErrorInfo} instances to {@link
   * GraphQLError} instances using {@link GraphQLUtils#convertToGraphQlError(GraphQLErrorInfo)}.
   *
   * @return List of all collected GraphQL errors
   */
  List<GraphQLError> getErrors();
}
