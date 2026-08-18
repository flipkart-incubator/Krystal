package com.flipkart.krystal.vajram.facets;

import static com.google.common.collect.Collections2.transform;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;

import com.flipkart.krystal.data.Errable;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.Nullable;

@Value
public class FanoutCommand<T> implements DependencyCommand<T> {
  Collection<Errable<T>> errables;
  boolean shouldSkip;
  String doc;
  private final @Nullable Throwable skipCause;

  private FanoutCommand(
      Collection<Errable<T>> values,
      boolean shouldSkip,
      String doc,
      @Nullable Throwable skipCause) {
    this.errables = unmodifiableCollection(values);
    this.shouldSkip = shouldSkip;
    this.doc = doc;
    this.skipCause = skipCause;
  }

  public static <T> FanoutCommand<T> executeFanoutWith(Collection<? extends T> values) {
    //noinspection unchecked
    return new FanoutCommand<>(
        ImmutableList.copyOf(transform(values, Errable::withValue)), false, EMPTY_STRING, null);
  }

  public static <T> FanoutCommand<T> fanoutWithErrables(Collection<Errable<T>> values) {
    //noinspection unchecked
    return new FanoutCommand<>(values, false, EMPTY_STRING, null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason) {
    return skipFanout(reason, null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason, @Nullable Throwable skipCause) {
    return new FanoutCommand<>(emptyList(), true, reason, skipCause);
  }
}
