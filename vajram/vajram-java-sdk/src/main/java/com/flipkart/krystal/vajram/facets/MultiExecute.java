package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class MultiExecute<T> implements DependencyCommand<T> {
  private final ImmutableList<T> inputs;
  private final boolean shouldSkip;
  private final String doc;

  public MultiExecute(Collection<T> inputs, boolean shouldSkip, String doc) {
    this.inputs = ImmutableList.copyOf(inputs);
    this.shouldSkip = shouldSkip;
    this.doc = doc;
  }

  @Override
  public ImmutableList<T> inputs() {
    return inputs;
  }

  @Override
  public boolean shouldSkip() {
    return shouldSkip;
  }

  @Override
  public String doc() {
    return doc;
  }

  public static <T> MultiExecute<T> executeFanoutWith(Collection<? extends T> inputs) {
    return new MultiExecute<>(
        inputs == null ? ImmutableList.of() : ImmutableList.copyOf(inputs), false, EMPTY_STRING);
  }

  public static <T> MultiExecute<T> skipFanout(String reason) {
    return new MultiExecute<>(Collections.emptyList(), true, reason);
  }
}
