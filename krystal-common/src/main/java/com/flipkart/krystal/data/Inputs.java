package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

public record Inputs(ImmutableMap<String, InputValue<Object>> values) {

  public Inputs(Map<String, InputValue<Object>> values) {
    this(ImmutableMap.copyOf(values));
  }

  private static final Inputs EMPTY = new Inputs(ImmutableMap.of());

  public InputValue<?> get(String inputName) {
    return values.getOrDefault(inputName, ValueOrError.empty());
  }

  public <T> ValueOrError<T> getInputValue(String inputName) {
    InputValue<?> inputValue = values.getOrDefault(inputName, ValueOrError.empty());
    if (inputValue instanceof ValueOrError<?> voe) {
      //noinspection unchecked
      return (ValueOrError<T>) voe;
    }
    throw new IllegalArgumentException("%s is not of type ValueOrError".formatted(inputName));
  }

  public <T> Optional<T> getInputValueOpt(String inputName) {
    return this.<T>getInputValue(inputName).value();
  }

  public <T> T getInputValueOrThrow(String inputName) {
    return this.<T>getInputValue(inputName).getValueOrThrow().orElseThrow();
  }

  public <T> T getInputValueOrDefault(String inputName, T defaultValue) {
    return this.<T>getInputValueOpt(inputName).orElse(defaultValue);
  }

  public <T> Results<T> getDepValue(String inputName) {
    InputValue<?> inputValue = values.getOrDefault(inputName, Results.empty());
    if (inputValue instanceof Results<?> voe) {
      //noinspection unchecked
      return (Results<T>) voe;
    }
    throw new IllegalArgumentException("%s is not of type Results".formatted(inputName));
  }

  public static Inputs union(
      Map<String, ? extends InputValue<Object>> inputs1,
      Map<String, ? extends InputValue<Object>> inputs2) {
    //noinspection UnstableApiUsage
    return new Inputs(
        ImmutableMap.<String, InputValue<Object>>builderWithExpectedSize(
                inputs1.size() + inputs2.size())
            .putAll(inputs1)
            .putAll(inputs2)
            .build());
  }

  public static Inputs empty() {
    return EMPTY;
  }

  @Override
  public String toString() {
    return values().toString();
  }
}
