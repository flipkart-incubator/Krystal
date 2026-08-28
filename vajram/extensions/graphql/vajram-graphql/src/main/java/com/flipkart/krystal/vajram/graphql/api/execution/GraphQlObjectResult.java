package com.flipkart.krystal.vajram.graphql.api.execution;

import static com.flipkart.krystal.data.Errable.errableFrom;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperation;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A special wrapper over graphQlOperationObject - this is to allow clients to extract the complete
 * ExecutionResult (with errors, extensions etc.) only if and when needed - leading to better
 * performance. For example, when using lattice graphqlServer, we need to extract the complete
 * ExecutionResult in the HttpApi Vajram so that the response type of the {@link
 * GraphQlOperationAggregate} dependency remains compliant with {@link GraphQlOperation}
 *
 * @param graphQlOperationObject the graphQlOperationObject returned by the invocation of {@link
 *     GraphQlOperationAggregate}
 */
public record GraphQlObjectResult(Errable<GraphQlOperation> graphQlOperationObject) {

  public GraphQlObjectResult(
      @Nullable GraphQlOperation graphQlOperation, @Nullable Throwable error) {
    this(errableFrom(graphQlOperation, error));
  }
}
