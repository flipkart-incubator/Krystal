package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.SingleValue;
import com.flipkart.krystal.krystex.Value;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public record NodeInputs(Map<String, Value> values) {

  public NodeInputs() {
    this(ImmutableMap.of());
  }

  public Value getValue(String inputName) {
    return values().getOrDefault(inputName, SingleValue.<Object>empty());
  }
}
