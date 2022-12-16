package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.inputs.SingleValue.empty;

import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.SingleValue;
import com.google.common.collect.ImmutableMap;

public record ExecutionContextMap(InputValues context) implements ExecutionContext {

  public <T> T getValue(String key) {
    //noinspection unchecked
    return (T) context().values().getOrDefault(key, empty()).value().orElseThrow();
  }

  public ImmutableMap<String, SingleValue<?>> asMap() {
    return context.values();
  }
}
