package com.flipkart.krystal.vajram.graphql.samples.query;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.samples.name.Name;
import com.flipkart.krystal.vajram.graphql.samples.order.Order;
import com.flipkart.krystal.vajram.graphql.samples.query.Query_Immut.Builder;
import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("return")
public final class Query_ImmutGQlRespJson implements Query_Immut {

  @Override
  public Query_ImmutGQlRespJson _newCopy() {
    return null;
  }

  @Override
  public @Nullable Order order() {
    return null;
  }

  @Override
  public @Nullable Name name() {
    return null;
  }

  @Override
  public Builder _asBuilder() {
    return null;
  }

  @Override
  public @Nullable String __typename() {
    return "";
  }

  @Override
  public ExecutionContext graphql_executionContext() {
    return null;
  }

  @Override
  public VajramExecutionStrategy graphql_executionStrategy() {
    return null;
  }

  @Override
  public ExecutionStrategyParameters graphql_executionStrategyParams() {
    return null;
  }

  @Override
  public @Nullable Map<String, Object> _data() {
    return Map.of();
  }

  @Override
  public @Nullable List<GraphQLError> _errors() {
    return List.of();
  }

  @Override
  public @Nullable Map<String, Object> _extensions() {
    return Map.of();
  }

  public static Builder _builder() {
    return new Builder();
  }

  public static final class Builder implements Query_Immut.Builder {

    @Override
    public Builder graphql_executionContext(ExecutionContext graphql_executionContext) {
      return null;
    }

    @Override
    public Builder graphql_executionStrategy(VajramExecutionStrategy graphql_executionStrategy) {
      return null;
    }

    @Override
    public Builder graphql_executionStrategyParams(
        ExecutionStrategyParameters graphql_executionStrategyParams) {
      return null;
    }

    @Override
    public Builder order(@Nullable Order order) {
      return null;
    }

    public Builder order(Errable<Order> order) {
      return null;
    }

    @Override
    public Builder name(@Nullable Name name) {
      return null;
    }

    public Builder name(Errable<Name> name) {
      return null;
    }

    @Override
    public Query_ImmutGQlRespJson _build() {
      return null;
    }

    @Override
    public Builder _newCopy() {
      return null;
    }

    @Override
    public @Nullable Order order() {
      return null;
    }

    @Override
    public @Nullable Name name() {
      return null;
    }

    @Override
    public @Nullable String __typename() {
      return "";
    }

    @Override
    public ExecutionContext graphql_executionContext() {
      return null;
    }

    @Override
    public VajramExecutionStrategy graphql_executionStrategy() {
      return null;
    }

    @Override
    public ExecutionStrategyParameters graphql_executionStrategyParams() {
      return null;
    }

    @Override
    public @Nullable Map<String, Object> _data() {
      return Map.of();
    }

    @Override
    public @Nullable List<GraphQLError> _errors() {
      return List.of();
    }

    @Override
    public @Nullable Map<String, Object> _extensions() {
      return Map.of();
    }
  }
}
