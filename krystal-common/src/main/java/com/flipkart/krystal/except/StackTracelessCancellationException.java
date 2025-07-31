package com.flipkart.krystal.except;

import java.util.concurrent.CancellationException;

/**
 * An exception whose stacktrace is not useful. For example, exceptions used for internal purposes
 * or for failing completable futures with a marker exception.
 *
 * <p>This class extends {@link CancellationException} so that CompletableFutures can be cancelled
 * without paying the price of filling the stack trace. This unnecessary wrapping has been observed
 * to have a negative impact on performance - in some instances taking up upto 6% of the total CPU
 * resources.
 *
 * @implNote This class overrides {@link Throwable#fillInStackTrace()} and skips filling the stack
 *     trace to improve performance.
 */
public class StackTracelessCancellationException extends CancellationException {

  private static final StackTracelessCancellationException INSTANCE =
      new StackTracelessCancellationException();

  private StackTracelessCancellationException() {}

  public StackTracelessCancellationException(String message) {
    super(message);
  }

  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  @Override
  public final Throwable fillInStackTrace() {
    // This exception is used to complete exceptions. Stack trace is not useful.
    return this;
  }

  public static StackTracelessCancellationException stackTracelessCancellation() {
    return INSTANCE;
  }
}
