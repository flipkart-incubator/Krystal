package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.data.ValueOrError.empty;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public record ExecutionContextMap(InputValues context) implements ExecutionContext {

  public <T> T getValue(String key) {
    return this.<T>optValue(key).orElseThrow();
  }

  public <T> T getValue(String key, T defaultValue) {
    return this.<T>optValue(key).orElse(defaultValue);
  }

  public <T> Optional<T> optValue(String key) {
    //noinspection unchecked
    return (Optional<T>) context().values().getOrDefault(key, empty()).value();
  }

  public ImmutableMap<String, ValueOrError<?>> asMap() {
    return context.values();
  }
}
