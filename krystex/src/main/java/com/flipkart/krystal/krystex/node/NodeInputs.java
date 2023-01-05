package com.flipkart.krystal.krystex.node;

import static com.flipkart.krystal.krystex.SingleValue.empty;

import com.flipkart.krystal.krystex.Value;
import com.google.common.collect.ImmutableMap;

public record NodeInputs(ImmutableMap<String, Value> values) {

  public NodeInputs() {
    this(ImmutableMap.of());
  }

  public Value getValue(String inputName) {
    return values().getOrDefault(inputName, empty());
  }

  //  public <T> Optional<T> get(String inputName) {
  //    //noinspection unchecked
  //    return Optional.ofNullable(this.values().get(inputName)).map(sv -> (T)
  // sv.value().orElse(null));
  //  }
}
