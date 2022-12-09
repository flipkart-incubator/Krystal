package com.flipkart.krystal.vajram;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

public final class ExecutionContextMap implements ExecutionContext {
  private final ImmutableMap<String, Object> context;

  public ExecutionContextMap(Map<String, Object> context) {
    this.context = ImmutableMap.copyOf(context);
  }

  public <T> T getValue(String key) {
    //noinspection unchecked
    return Optional.ofNullable((T) context.get(key)).orElseThrow();
  }

  public ImmutableMap<String, ?> asMap() {
    return context;
  }
}
