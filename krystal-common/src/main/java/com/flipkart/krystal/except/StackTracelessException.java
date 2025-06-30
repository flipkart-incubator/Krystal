package com.flipkart.krystal.except;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An exception whose stacktrace is not useful. For example, exceptions used for internal purposes
 * or for failing completable futures with a marker exception.
 *
 * @implNote This class overrides {@link Throwable#fillInStackTrace()} and skips filling the stack
 *     trace to improve performance.
 */
public class StackTracelessException extends RuntimeException {

  protected StackTracelessException() {
    this(null);
  }

  public StackTracelessException(@Nullable String message) {
    this(message, null);
  }

  public StackTracelessException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause, true, false);
  }
}
