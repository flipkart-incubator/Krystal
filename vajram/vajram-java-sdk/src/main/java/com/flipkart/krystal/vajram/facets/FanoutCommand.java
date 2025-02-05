package com.flipkart.krystal.vajram.facets;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;

public record FanoutCommand<T>(
    ImmutableList<T> inputs, boolean shouldSkip, String doc, @Nullable Throwable skipCause)
    implements DependencyCommand<T> {

  public FanoutCommand(
      Collection<T> inputs, boolean shouldSkip, String doc, @Nullable Throwable skipCause) {
    this(ImmutableList.copyOf(inputs), shouldSkip, doc, skipCause);
  }

  @Override
  public String doc() {
    return doc;
  }

  public static <T> FanoutCommand<T> executeFanoutWith(Collection<? extends T> inputs) {
    return new FanoutCommand<>(
        inputs == null ? ImmutableList.of() : ImmutableList.copyOf(inputs),
        false,
        EMPTY_STRING,
        null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason) {
    return skipFanout(reason, null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason, @Nullable Throwable skipCause) {
    return new FanoutCommand<>(Collections.emptyList(), true, reason, skipCause);
  }
}
