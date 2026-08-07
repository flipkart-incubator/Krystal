package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlObjectMap.GraphQlObjectMapBuilder;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GraphQlOperationObjectMap extends GraphQlObjectMap
    implements GraphQlOperationObject {

  private final @Nullable Map<Object, Object> extensions;

  public GraphQlOperationObjectMap(
      ExecutionContext graphql_executionContext,
      VajramExecutionStrategy graphql_executionStrategy,
      ExecutionStrategyParameters graphql_executionStrategyParams,
      Map<String, GraphQlValue> graphql_values) {
    this(
        graphql_executionContext,
        graphql_executionStrategy,
        graphql_executionStrategyParams,
        graphql_values,
        null);
  }

  public GraphQlOperationObjectMap(
      ExecutionContext graphql_executionContext,
      VajramExecutionStrategy graphql_executionStrategy,
      ExecutionStrategyParameters graphql_executionStrategyParams,
      Map<String, GraphQlValue> graphql_values,
      @Nullable Map<Object, Object> extensions) {
    super(
        graphql_executionContext,
        graphql_executionStrategy,
        graphql_executionStrategyParams,
        graphql_values);
    this.extensions = extensions;
  }

  @Override
  public @Nullable Map<Object, Object> graphql_extensions() {
    return extensions;
  }

  public static class GraphQlOperationObjectMapBuilder
      extends GraphQlObjectMapBuilder<GraphQlOperationObjectMapBuilder> {

    private @Nullable Map<Object, Object> extensions;

    public GraphQlOperationObjectMapBuilder addExtension(Object key, Object value) {
      if (extensions == null) {
        extensions = new LinkedHashMap<>();
      }
      extensions.put(key, value);
      return this;
    }

    public GraphQlOperationObjectMap build() {
      return new GraphQlOperationObjectMap(
          graphql_executionContext,
          graphql_executionStrategy,
          graphql_executionStrategyParams,
          graphql_data,
          extensions);
    }
  }
}
