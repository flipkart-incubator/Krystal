package com.flipkart.krystal.vajram.graphql.samples.hello;

import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Hello_ImmutGQlRespJson implements Hello_Immut {

  public static Hello_ImmutGQlRespJson.Builder _builder() {
    return new Builder();
  }

  @Override
  public Hello_Immut _newCopy() {
    return null;
  }

  @Override
  public Builder _asBuilder() {
    return null;
  }

  @Override
  public String toName() {
    return "";
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

  public static final class Builder implements Hello_Immut.Builder {

    @Override
    public Builder toName(String name) {
      return null;
    }

    @Override
    public Hello_Immut _build() {
      return null;
    }

    @Override
    public Builder _newCopy() {
      return null;
    }

    public Builder graphql_executionContext(ExecutionContext graphqlExecutionContext) {
      return null;
    }

    public Builder graphql_executionStrategy(VajramExecutionStrategy graphqlExecutionStrategy) {
      return null;
    }

    public Builder graphql_executionStrategyParams(ExecutionStrategyParameters dummies) {
      return null;
    }

    @Override
    public @Nullable String toName() {
      return "";
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
