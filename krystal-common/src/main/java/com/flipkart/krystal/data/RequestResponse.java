package com.flipkart.krystal.data;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents the results of a depdendency invocation. */
public record RequestResponse<R extends Request<T>, T>(R request, Errable<T> response)
    implements One2OneDepResponse<R, T> {

  @Override
  public @Nullable T value() {
    return response.value();
  }
}
