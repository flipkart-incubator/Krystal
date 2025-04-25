package com.flipkart.krystal.vajram.facets;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public record FanoutCommand<T>(
    List<@Nullable T> inputs, boolean shouldSkip, String doc, @Nullable Throwable skipCause)
    implements DependencyCommand<T> {

  public FanoutCommand(
      Collection<? extends T> inputs,
      boolean shouldSkip,
      String doc,
      @Nullable Throwable skipCause) {
    this(new ArrayList<>(inputs), shouldSkip, doc, skipCause);
  }

  public FanoutCommand {
    inputs = unmodifiableList(inputs);
  }

  @Override
  public String doc() {
    return doc;
  }

  public static <T> FanoutCommand<T> executeFanoutWith(Collection<? extends T> inputs) {
    return new FanoutCommand<>(inputs, false, EMPTY_STRING, null);
  }

  @SuppressWarnings("unchecked")
  public static <T> FanoutCommand<T> executeFanoutWith(List<? extends T> inputs) {
    return new FanoutCommand<>((List<@Nullable T>) inputs, false, EMPTY_STRING, null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason) {
    return skipFanout(reason, null);
  }

  public static <T> FanoutCommand<T> skipFanout(String reason, @Nullable Throwable skipCause) {
    return new FanoutCommand<>(Collections.<@Nullable T>emptyList(), true, reason, skipCause);
  }
}
