package com.flipkart.krystal.except;

import static com.flipkart.krystal.except.KrystalException.StackTracingStrategy.DEFAULT;
import static com.flipkart.krystal.except.KrystalException.StackTracingStrategy.DONT_FILL;
import static java.util.Objects.requireNonNullElse;

import java.util.concurrent.CompletionException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An exception thrown during execution of a Krystal Graph. This class provides some additional
 * capabilities aimed at standardization and performance.
 *
 * <p>KrystalExceptions allow users to control whether a stacktrace needs to be generated. For
 * example, exceptions used for internal purposes or for failing completable futures with a marker
 * exception might not need a stacktrace, especially in production settings where creating
 * stacktraces can take up significant CPU. But for specific requests/tasks which need are selected
 * for debugging manually, or which are selected via sampling, KrystalExceptions can be configured
 * to generate stacktraces always. This makes debugging easier, at the same time preventing
 * unnecessary production impact
 *
 * <p>This class extends {@link CompletionException} so that CompletableFutures do not unnecessarily
 * wrap this exception in another CompletionException. This unnecessary wrapping has been observed
 * to have a negative impact on performance - in some instances taking up upto 20% of the total CPU
 * resources.
 *
 * @implNote This class overrides {@link Throwable#fillInStackTrace()} and skips filling the stack
 *     trace to improve performance.
 */
public class KrystalException extends CompletionException {

  private static final ThreadLocal<@Nullable StackTracingStrategy> STACK_TRACING_STRATEGY =
      new ThreadLocal<>();

  private static final StackTracingStrategy DEFAULT_STACK_TRACING_STRATEGY =
      stackTracingSystemProperty();

  protected KrystalException() {
    this(null);
  }

  public KrystalException(@Nullable String message) {
    this(message, null);
  }

  public KrystalException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  @SuppressWarnings("NonSynchronizedMethodOverridesSynchronizedMethod")
  @Override
  public final Throwable fillInStackTrace() {
    return switch (getStackTracingStrategyForCurrentThread()) {
      case FILL -> super.fillInStackTrace();
      case DEFAULT, DONT_FILL -> this;
    };
  }

  /**
   * Sets the stack tracing strategy for the current thread and returns the previous strategy.
   *
   * @param stackTracingStrategy
   * @return The stack tracing strategy prior to this set.
   */
  public static @Nullable StackTracingStrategy setStackTracingStrategyForCurrentThread(
      StackTracingStrategy stackTracingStrategy) {
    StackTracingStrategy oldValue = STACK_TRACING_STRATEGY.get();
    STACK_TRACING_STRATEGY.set(stackTracingStrategy);
    return oldValue;
  }

  static StackTracingStrategy getStackTracingStrategyForCurrentThread() {
    return requireNonNullElse(STACK_TRACING_STRATEGY.get(), DEFAULT_STACK_TRACING_STRATEGY);
  }

  public static CompletionException wrapAsCompletionException(Throwable t) {
    if (t instanceof CompletionException c) {
      return c;
    }
    String message = t.getMessage();
    return new KrystalException(message != null ? message : t.getClass().getName(), t);
  }

  public enum StackTracingStrategy {
    DEFAULT,
    FILL,
    DONT_FILL;
  }

  private static StackTracingStrategy stackTracingSystemProperty() {
    try {
      return StackTracingStrategy.valueOf(
          System.getProperty("KrystalException.defaultStackTracingStrategy", DONT_FILL.name()));
    } catch (IllegalArgumentException e) {
      return DEFAULT;
    }
  }
}
