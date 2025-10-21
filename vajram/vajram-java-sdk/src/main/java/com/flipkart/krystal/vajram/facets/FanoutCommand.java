package com.flipkart.krystal.vajram.facets;

import static java.util.Collections.unmodifiableCollection;

import java.util.Collection;
import java.util.Collections;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;

@Value
public class FanoutCommand<T> implements DependencyCommand<T> {
  Collection<? extends T> values;
  boolean shouldSkip;
  String doc;
  @Nullable Throwable skipCause;

  private FanoutCommand(
      Collection<? extends T> values,
      boolean shouldSkip,
      String doc,
      @Nullable Throwable skipCause) {
    this.values = unmodifiableCollection(values);
    this.shouldSkip = shouldSkip;
    this.doc = doc;
    this.skipCause = skipCause;
  }

  public static <T> FanoutCommand<T> executeFanoutWith(Collection<? extends T> values) {
    //noinspection unchecked
    return new FanoutCommand<>(values, false, EMPTY_STRING, null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason) {
    return skipFanout(reason, null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason, @Nullable Throwable skipCause) {
    return new FanoutCommand<>(Collections.<@Nullable T>emptyList(), true, reason, skipCause);
  }
}
