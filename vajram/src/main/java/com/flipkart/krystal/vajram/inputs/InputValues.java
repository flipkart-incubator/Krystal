package com.flipkart.krystal.vajram.inputs;

import static com.flipkart.krystal.data.ValueOrError.empty;

import com.flipkart.krystal.data.ValueOrError;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record InputValues(ImmutableMap<String, ValueOrError<?>> values) {

  public InputValues() {
    this(ImmutableMap.of());
  }

  public <T> ValueOrError<T> getValue(String inputName) {
    //noinspection unchecked
    return (ValueOrError<T>) values().getOrDefault(inputName, empty());
  }

  public <T> Optional<T> getOpt(String inputName) {
    return this.<T>getValue(inputName).value();
  }

  public <T> T getOrThrow(String inputName) {
    return this.<T>getOpt(inputName).orElseThrow();
  }

  public <T> T getOrDefault(String inputName, T defaultValue) {
    return this.<T>getOpt(inputName).orElse(defaultValue);
  }
}
