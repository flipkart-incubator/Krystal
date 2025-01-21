package com.flipkart.krystal.except;

/**
 * An exception whose stacktrace is not useful. For example, exceptions used for internal purposes
 * or for failing completable futures with a marker exception.
 *
 * @implNote This class overrides {@link Throwable#fillInStackTrace()} and skips filling the stack
 *     trace to improve performance.
 */
public class StackTracelessException extends RuntimeException {

  protected StackTracelessException() {}

  public StackTracelessException(String message) {
    super(message);
  }

  public StackTracelessException(String message, Exception cause) {
    super(message, cause);
  }

  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  @Override
  public final Throwable fillInStackTrace() {
    // This exception is used to complete exceptions. Stack trace is not useful.
    return this;
  }
}
