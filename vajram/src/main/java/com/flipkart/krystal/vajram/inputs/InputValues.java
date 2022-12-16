package com.flipkart.krystal.vajram.inputs;

import static com.flipkart.krystal.vajram.inputs.SingleValue.empty;

import com.google.common.collect.ImmutableMap;

public record InputValues(ImmutableMap<String, SingleValue<?>> values) {

  public InputValues() {
    this(ImmutableMap.of());
  }

  public <T> SingleValue<T> getValue(String inputName) {
    //noinspection unchecked
    return (SingleValue<T>) values().getOrDefault(inputName, empty());
  }
}
