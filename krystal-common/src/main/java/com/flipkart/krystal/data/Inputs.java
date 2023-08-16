package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;

@EqualsAndHashCode(callSuper = false, cacheStrategy = CacheStrategy.LAZY)
public final class Inputs {
  private final ImmutableMap<String, InputValue<Object>> values;

  public Inputs(Map<String, InputValue<Object>> values) {
    this.values = ImmutableMap.copyOf(values);
  }

  private static final Inputs EMPTY = new Inputs(ImmutableMap.of());

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

  public <T> @NonNull T getInputValueOrThrow(String inputName) {
    return this.<@NonNull T>getInputValue(inputName)
        .getValueOrThrow()
        .orElseThrow(
            () -> new IllegalStateException("Could not find input value %s".formatted(inputName)));
  }

  public <T> @PolyNull T getInputValueOrDefault(String inputName, @PolyNull T defaultValue) {
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

  public ImmutableMap<String, InputValue<Object>> values() {
    return values;
  }

  @Override
  public String toString() {
    return values().toString();
  }
}
