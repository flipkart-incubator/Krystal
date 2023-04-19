package com.flipkart.krystal.krystex.decoration;

import static java.util.Comparator.comparingInt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LogicDecorationOrdering {

  private final ImmutableMap<String, Integer> decoratorTypeIndices;

  public LogicDecorationOrdering(ImmutableSet<String> orderedDecoratorIds) {
    List<String> strings = orderedDecoratorIds.stream().toList();
    Map<String, Integer> indices = new HashMap<>();
    for (int i = 0; i < orderedDecoratorIds.size(); i++) {
      indices.put(strings.get(i), i);
    }
    this.decoratorTypeIndices = ImmutableMap.copyOf(indices);
  }

  public Comparator<LogicDecorator<?, ?>> decorationOrder() {
    return comparingInt(
        key ->
            Optional.ofNullable(decoratorTypeIndices.get(key.decoratorType()))
                .orElse(Integer.MIN_VALUE));
  }
}
