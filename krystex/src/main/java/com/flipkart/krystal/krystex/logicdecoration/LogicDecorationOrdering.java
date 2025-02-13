package com.flipkart.krystal.krystex.logicdecoration;

import static java.util.Comparator.comparingInt;
import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.krystex.Decorator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(cacheStrategy = LAZY)
public class LogicDecorationOrdering {

  private static final LogicDecorationOrdering EMPTY =
      new LogicDecorationOrdering(ImmutableSet.of());

  private final ImmutableMap<String, Integer> decoratorTypeIndices;

  /**
   * @param orderedDecoratorIds The first id in this list will process the command first and the
   *     command response last in relation to later decorator ids.
   */
  public LogicDecorationOrdering(ImmutableSet<String> orderedDecoratorIds) {
    List<String> strings = orderedDecoratorIds.stream().toList();
    Map<String, Integer> indices = new HashMap<>();
    for (int i = 0; i < orderedDecoratorIds.size(); i++) {
      indices.put(strings.get(i), i);
    }
    this.decoratorTypeIndices = ImmutableMap.copyOf(indices);
  }

  public <T extends Decorator> Comparator<T> encounterOrder() {
    return comparingInt(
        key -> decoratorTypeIndices.getOrDefault(key.decoratorType(), Integer.MIN_VALUE));
  }

  public static LogicDecorationOrdering none() {
    return EMPTY;
  }
}
