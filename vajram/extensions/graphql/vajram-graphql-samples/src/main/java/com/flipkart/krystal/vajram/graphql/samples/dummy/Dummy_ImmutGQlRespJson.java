package com.flipkart.krystal.vajram.graphql.samples.dummy;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Immut.Builder;
import com.flipkart.krystal.vajram.graphql.samples.order.Order;
import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("return")
public class Dummy_ImmutGQlRespJson implements Dummy_Immut {

  public static Dummy_ImmutGQlRespJson.Builder _builder() {
    return new Builder();
  }

  @Override
  public Dummy_Immut _newCopy() {
    return null;
  }

  @Override
  public Builder _asBuilder() {
    return null;
  }

  @Override
  public DummyId id() {
    return null;
  }

  @Override
  public String name() {
    return "";
  }

  @Override
  public @Nullable Integer age() {
    return 0;
  }

  @Override
  public @Nullable Order order() {
    return null;
  }

  @Override
  public @Nullable String f1() {
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

  public static final class Builder implements Dummy_Immut.Builder {

    @Override
    public Builder id(DummyId id) {
      return null;
    }

    @Override
    public Builder name(String name) {
      return null;
    }

    @Override
    public Builder age(@Nullable Integer age) {
      return null;
    }

    @Override
    public Builder order(@Nullable Order order) {
      return null;
    }

    @Override
    public Dummy_Immut.Builder f1(@Nullable String f1) {
      return null;
    }

    public Builder order(Errable<Order> order) {
      return null;
    }

    @Override
    public Dummy_Immut _build() {
      return null;
    }

    @Override
    public Builder _newCopy() {
      return null;
    }

    @Override
    public DummyId id() {
      return null;
    }

    @Override
    public String name() {
      return "";
    }

    @Override
    public @Nullable Integer age() {
      return 0;
    }

    @Override
    public @Nullable Order order() {
      return null;
    }

    @Override
    public @Nullable String f1() {
      return "";
    }

    @Override
    public Builder graphql_executionContext(ExecutionContext graphqlExecutionContext) {
      return null;
    }

    @Override
    public Builder graphql_executionStrategy(VajramExecutionStrategy graphqlExecutionStrategy) {
      return null;
    }

    @Override
    public Builder graphql_executionStrategyParams(ExecutionStrategyParameters dummies) {
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
