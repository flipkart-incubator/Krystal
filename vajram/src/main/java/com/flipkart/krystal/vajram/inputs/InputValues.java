package com.flipkart.krystal.vajram.inputs;

import static com.flipkart.krystal.vajram.inputs.ValueOrError.empty;

import com.google.common.collect.ImmutableMap;

public record InputValues(ImmutableMap<String, ValueOrError<?>> values) {

  public InputValues() {
    this(ImmutableMap.of());
  }

  public <T> ValueOrError<T> getValue(String inputName) {
    //noinspection unchecked
    return (ValueOrError<T>) values().getOrDefault(inputName, empty());
  }
}
