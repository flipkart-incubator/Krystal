package com.flipkart.krystal.krystex.decoration;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import org.checkerframework.common.returnsreceiver.qual.This;

@EqualsAndHashCode(cacheStrategy = LAZY)
public class DecorationOrdering {

  private static final DecorationOrdering EMPTY =
      new DecorationOrdering(ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of());

  private final ImmutableMap<String, Integer> kryonDecoratorIndices;
  private final ImmutableMap<String, Integer> outputLogicDecoratorIndices;
  private final ImmutableMap<String, Integer> dependencyDecoratorIndices;

  /**
   * The first id in this list will process the command first and the command response last in
   * relation to later decorator ids.
   */
  public DecorationOrdering(
      ImmutableSet<String> kryonDecoratorOrdering,
      ImmutableSet<String> outputLogicDecoratorOrdering,
      ImmutableSet<String> dependencyDecoratorOrdering) {
    this.kryonDecoratorIndices = indexDecorators(kryonDecoratorOrdering);
    this.outputLogicDecoratorIndices = indexDecorators(outputLogicDecoratorOrdering);
    this.dependencyDecoratorIndices = indexDecorators(dependencyDecoratorOrdering);
  }

  public ImmutableMap<String, Integer> kryonDecoratorIndices() {
    return kryonDecoratorIndices;
  }

  public ImmutableMap<String, Integer> outputLogicDecoratorIndices() {
    return outputLogicDecoratorIndices;
  }

  public ImmutableMap<String, Integer> dependencyDecoratorIndices() {
    return dependencyDecoratorIndices;
  }

  public <T extends Decorator> Comparator<T> encounterOrder() {
    return Comparator.<T>comparingInt(
            key -> kryonDecoratorIndices.getOrDefault(key.decoratorType(), Integer.MIN_VALUE))
        .thenComparing(Decorator::decoratorType);
  }

  public static DecorationOrdering none() {
    return EMPTY;
  }

  private static ImmutableMap<String, Integer> indexDecorators(
      ImmutableSet<String> decoratorOrdering) {
    Map<String, Integer> indices = new HashMap<>();
    int i = 0;
    for (String decoratorType : decoratorOrdering) {
      indices.put(decoratorType, i++);
    }
    return ImmutableMap.copyOf(indices);
  }

  public static DecorationOrderingBuilder builder() {
    return new DecorationOrderingBuilder();
  }

  public static class DecorationOrderingBuilder {
    private ImmutableSet<String> kryonDecoratorOrdering = ImmutableSet.of();
    private ImmutableSet<String> outputLogicDecoratorOrdering = ImmutableSet.of();
    private ImmutableSet<String> dependencyDecoratorOrdering = ImmutableSet.of();

    @This
    public DecorationOrderingBuilder kryonDecoratorOrdering(String... kryonDecoratorOrdering) {
      this.kryonDecoratorOrdering = ImmutableSet.copyOf(kryonDecoratorOrdering);
      return this;
    }

    @This
    public DecorationOrderingBuilder outputLogicDecoratorOrdering(
        String... outputLogicDecoratorOrdering) {
      this.outputLogicDecoratorOrdering = ImmutableSet.copyOf(outputLogicDecoratorOrdering);
      return this;
    }

    @This
    public DecorationOrderingBuilder dependencyDecoratorOrdering(
        String... dependencyDecoratorOrdering) {
      this.dependencyDecoratorOrdering = ImmutableSet.copyOf(dependencyDecoratorOrdering);
      return this;
    }

    public DecorationOrdering build() {
      return new DecorationOrdering(
          kryonDecoratorOrdering, outputLogicDecoratorOrdering, dependencyDecoratorOrdering);
    }
  }
}
