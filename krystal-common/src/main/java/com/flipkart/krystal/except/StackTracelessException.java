package com.flipkart.krystal.except;

import java.util.concurrent.CompletionException;

/**
 * An exception whose stacktrace is not useful. For example, exceptions used for internal purposes
 * or for failing completable futures with a marker exception.
 *
 * <p>This class extends {@link CompletionException} so that CompletableFutures do not unnecessarily
 * wrap this exception in another CompletionException. This unnecessary wrapping has been observed
 * to have a negative impact on performance - in some instances taking up upto 20% of the total CPU
 * resources.
 *
 * @implNote This class overrides {@link Throwable#fillInStackTrace()} and skips filling the stack
 *     trace to improve performance.
 */
public class StackTracelessException extends CompletionException {

  public StackTracelessException(String message) {
    super(message);
  }

  public StackTracelessException(String message, Throwable cause) {
    super(message, cause);
  }

  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  @Override
  public final Throwable fillInStackTrace() {
    // This exception is used to complete exceptions. Stack trace is not useful.
    return this;
  }

  public static CompletionException stackTracelessWrap(Throwable t) {
    if (t instanceof CompletionException) {
      return (CompletionException) t;
    }
    String message = t.getMessage();
    return new StackTracelessException(message != null ? message : t.getClass().getName(), t);
  }
}
