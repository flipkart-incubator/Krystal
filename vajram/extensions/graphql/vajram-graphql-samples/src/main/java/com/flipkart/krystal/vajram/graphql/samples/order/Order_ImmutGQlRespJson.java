package com.flipkart.krystal.vajram.graphql.samples.order;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.graphql.api.GraphQLUtils;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityModel_Immut;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Immut;
import com.flipkart.krystal.vajram.graphql.samples.hello.Hello;
import com.flipkart.krystal.vajram.graphql.samples.hello.Hello_Immut;
import com.flipkart.krystal.vajram.graphql.samples.name.Name;
import graphql.GraphQLError;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({
  "initialization.field.uninitialized",
  "initialization.fields.uninitialized",
  "return"
})
public final class Order_ImmutGQlRespJson
    implements Order_Immut, GraphQlEntityModel_Immut<OrderId> {
  private final ExecutionContext graphql_executionContext;
  private final VajramExecutionStrategy graphql_executionStrategy;
  private final ExecutionStrategyParameters graphql_executionStrategyParams;

  private Errable<OrderId> id;
  private Errable<List<Errable<String>>> orderItemNames;
  private Errable<String> name;
  private Errable<Dummy_Immut> dummy;
  private Errable<List<Errable<Dummy_Immut>>> dummies;
  private Errable<Hello_Immut> hello;

  public Order_ImmutGQlRespJson(
      ExecutionContext graphql_executionContext,
      VajramExecutionStrategy graphql_executionStrategy,
      ExecutionStrategyParameters graphql_executionStrategyParams,
      Errable<? extends OrderId> id,
      Errable<List<Errable<String>>> orderItemNames,
      Errable<String> name,
      Errable<? extends Dummy> dummy,
      Errable<? extends List<? extends Errable<? extends Dummy>>> dummies,
      Errable<? extends Hello> hello) {
    this.graphql_executionContext = graphql_executionContext;
    this.graphql_executionStrategy = graphql_executionStrategy;
    this.graphql_executionStrategyParams = graphql_executionStrategyParams;
    if (graphql_executionContext == null
        || graphql_executionStrategy == null
        || graphql_executionStrategyParams == null) {
      throw new IllegalArgumentException(
          "graphql_executionContext, graphql_executionStrategy or graphql_executionStrategyParams cannot be null");
    }
    dummies.handle(
        _failure -> this.dummies = _failure.cast(),
        () -> this.dummies = Errable.nil(),
        _nonNil -> {
          List<Errable<Dummy_Immut>> _immutables = new ArrayList<>(_nonNil.value().size());
          for (Errable<? extends Dummy> _value : _nonNil.value()) {
            _value.handle(
                _failure -> _immutables.add(_failure.cast()),
                () -> _immutables.add(Errable.nil()),
                _nonNil2 ->
                    _immutables.add(
                        Errable.withValue(
                            _nonNil2
                                .value()
                                ._asBuilder()
                                .graphql_executionContext(graphql_executionContext)
                                .graphql_executionStrategy(graphql_executionStrategy)
                                .graphql_executionStrategyParams(
                                    graphql_executionStrategy.newParametersForFieldExecution(
                                        graphql_executionContext,
                                        graphql_executionStrategyParams,
                                        graphql_executionStrategyParams
                                            .getFields()
                                            .getSubField("dummies")))
                                ._build())));
          }
          this.dummies = Errable.withValue(_immutables);
        });
  }

  @Override
  public Order_ImmutGQlRespJson _newCopy() {
    return new Order_ImmutGQlRespJson(
        graphql_executionContext,
        graphql_executionStrategy,
        graphql_executionStrategyParams,
        id,
        orderItemNames,
        name,
        dummy,
        dummies,
        hello);
  }

  @Override
  public Order_Immut.Builder _asBuilder() {
    return null;
  }

  @Override
  public OrderId id() {
    return null;
  }

  @Override
  public List<@Nullable String> orderItemNames() {
    return List.of();
  }

  @Override
  public @Nullable String nameString() {
    return "";
  }

  @Override
  public @Nullable Dummy dummy() {
    return null;
  }

  @Override
  public List<Dummy> dummies() {
    return List.of();
  }

  @Override
  public @Nullable Hello hello() {
    return null;
  }

  @Override
  public List<Hello> recommendedHellos() {
    return List.of();
  }

  @Override
  public List<@Nullable Order> recommendedOrders() {
    return List.of();
  }

  @Override
  public @Nullable Hello hello2() {
    return null;
  }

  @Override
  public @Nullable Name name() {
    return null;
  }

  @Override
  public @Nullable String __typename() {
    if (GraphQLUtils.isFieldQueriedInTheNestedType("__typename", graphql_executionStrategyParams)) {
      return Order.class.getSimpleName();
    }
    return null;
  }

  @Override
  public ExecutionContext graphql_executionContext() {
    return graphql_executionContext;
  }

  @Override
  public VajramExecutionStrategy graphql_executionStrategy() {
    return graphql_executionStrategy;
  }

  @Override
  public ExecutionStrategyParameters graphql_executionStrategyParams() {
    return graphql_executionStrategyParams;
  }

  @Override
  public @Nullable Map<String, Object> _data() {
    // TBD
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable List<GraphQLError> _errors() {
    // TBD
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable Map<String, Object> _extensions() {
    // TBD
    throw new UnsupportedOperationException();
  }

  public static Builder _builder() {
    return new Builder();
  }

  public static final class Builder implements Order_Immut.Builder {

    private ExecutionContext graphql_executionContext;
    private VajramExecutionStrategy graphql_executionStrategy;
    private ExecutionStrategyParameters graphql_executionStrategyParams;

    private Errable<OrderId> id;
    private Errable<List<Errable<String>>> orderItemNames;
    private Errable<String> name;
    private Errable<Dummy> dummy;
    private Errable<? extends List<? extends Errable<? extends Dummy>>> dummies;
    private Errable<Hello> hello;

    @Override
    public Builder graphql_executionContext(ExecutionContext graphql_executionContext) {
      this.graphql_executionContext = graphql_executionContext;
      return this;
    }

    @Override
    public Builder graphql_executionStrategy(VajramExecutionStrategy graphql_executionStrategy) {
      this.graphql_executionStrategy = graphql_executionStrategy;
      return this;
    }

    @Override
    public Builder graphql_executionStrategyParams(
        ExecutionStrategyParameters graphql_executionStrategyParams) {
      this.graphql_executionStrategyParams = graphql_executionStrategyParams;
      return this;
    }

    @Override
    public Builder id(@Nullable OrderId id) {
      this.id = Errable.withValue(id);
      return this;
    }

    public Builder id(Errable<OrderId> id) {
      this.id = id;
      return this;
    }

    @Override
    public Builder orderItemNames(@Nullable List<@Nullable String> orderItemNames) {
      if (orderItemNames == null) {
        this.orderItemNames = Errable.nil();
        return this;
      }
      List<Errable<String>> _result = new ArrayList<>(orderItemNames.size());
      for (String orderItemName : orderItemNames) {
        _result.add(Errable.withValue(orderItemName));
      }
      this.orderItemNames = Errable.withValue(_result);
      return this;
    }

    @Override
    public Builder nameString(@Nullable String nameString) {
      return null;
    }

    public Builder nameString(Errable<String> nameString) {
      return null;
    }

    public Builder orderItemNames(Errable<List<Errable<String>>> orderItemNames) {
      this.orderItemNames = orderItemNames;
      return this;
    }

    public Builder name(Errable<String> name) {
      this.name = name;
      return this;
    }

    @Override
    public Builder dummy(@Nullable Dummy dummy) {
      return this;
    }

    public Builder dummy(Errable<Dummy> dummy) {
      this.dummy = dummy;
      return this;
    }

    @Override
    public Builder dummies(List<Dummy> dummies) {
      return null;
    }

    public Builder dummies(Errable<? extends List<? extends Errable<? extends Dummy>>> dummies) {
      this.dummies = dummies;
      return this;
    }

    @Override
    public Builder hello(@Nullable Hello hello) {
      return this;
    }

    @Override
    public Order_Immut.Builder recommendedHellos(List<Hello> recommendedHellos) {
      return null;
    }

    @Override
    public Order_Immut.Builder recommendedOrders(List<@Nullable Order> recommendedOrders) {
      return null;
    }

    @Override
    public Order_Immut.Builder hello2(@Nullable Hello hello2) {
      return null;
    }

    @Override
    public Order_Immut.Builder name(@Nullable Name name) {
      return null;
    }

    public Builder hello(Errable<Hello> hello) {
      this.hello = hello;
      return this;
    }

    @Override
    public Order_ImmutGQlRespJson _build() {
      return new Order_ImmutGQlRespJson(
          graphql_executionContext,
          graphql_executionStrategy,
          graphql_executionStrategyParams,
          id,
          orderItemNames,
          name,
          dummy,
          dummies,
          hello);
    }

    @Override
    public Order_Immut.Builder _newCopy() {
      return Order_ImmutGQlRespJson._builder()
          .graphql_executionContext(graphql_executionContext)
          .graphql_executionStrategy(graphql_executionStrategy)
          .graphql_executionStrategyParams(graphql_executionStrategyParams)
          .id(id)
          .orderItemNames(orderItemNames)
          .name(name)
          .dummy(dummy)
          .dummies(dummies)
          .hello(hello);
    }

    @Override
    public OrderId id() {
      return id.valueOpt().orElse(null);
    }

    @Override
    public List<@Nullable String> orderItemNames() {
      return List.of();
    }

    @Override
    public @Nullable String nameString() {
      return "";
    }

    @Override
    public @Nullable Dummy dummy() {
      return null;
    }

    @Override
    public List<Dummy> dummies() {
      return List.of();
    }

    @Override
    public @Nullable Hello hello() {
      return null;
    }

    @Override
    public List<Hello> recommendedHellos() {
      return List.of();
    }

    @Override
    public List<@Nullable Order> recommendedOrders() {
      return List.of();
    }

    @Override
    public @Nullable Hello hello2() {
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
      return graphql_executionContext;
    }

    @Override
    public VajramExecutionStrategy graphql_executionStrategy() {
      return graphql_executionStrategy;
    }

    @Override
    public ExecutionStrategyParameters graphql_executionStrategyParams() {
      return graphql_executionStrategyParams;
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
