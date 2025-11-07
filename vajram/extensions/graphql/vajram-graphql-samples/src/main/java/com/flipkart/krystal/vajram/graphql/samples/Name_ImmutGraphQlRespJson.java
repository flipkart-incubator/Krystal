package com.flipkart.krystal.vajram.graphql.samples;

import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.samples.name.Name_Immut;
import com.flipkart.krystal.vajram.graphql.samples.name.Name_Immut.Builder;
import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Name_ImmutGraphQlRespJson implements Name_Immut {

  @Override
  public Name_Immut _newCopy() {
    return null;
  }

  @Override
  public @Nullable String value() {
    return "";
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

  public static final class Builder implements Name_Immut.Builder {

    @Override
    public Name_Immut.Builder graphql_executionContext(ExecutionContext graphql_executionContext) {
      return null;
    }

    @Override
    public Name_Immut.Builder graphql_executionStrategy(
        VajramExecutionStrategy graphql_executionStrategy) {
      return null;
    }

    @Override
    public Name_Immut.Builder graphql_executionStrategyParams(
        ExecutionStrategyParameters graphql_executionStrategyParams) {
      return null;
    }

    @Override
    public Name_Immut.Builder value(@Nullable String value) {
      return null;
    }

    @Override
    public Name_Immut _build() {
      return null;
    }

    @Override
    public Name_Immut.Builder _newCopy() {
      return null;
    }

    @Override
    public @Nullable String value() {
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
