package com.flipkart.krystal.except;

/**
 * @see KrystalCompletionException
 * @see KrystalCancellationException
 */
public enum StackTracingStrategy {
  DEFAULT,
  /**
   * Fill stacktrace for exceptions on exception creation - better debuggability, worse performance.
   */
  FILL,
  /**
   * Don't fill stacktrace for exceptions on exception creation - worse debuggability, better
   * performance.
   */
  DONT_FILL;
}
