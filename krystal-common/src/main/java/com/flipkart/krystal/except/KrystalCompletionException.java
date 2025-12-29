package com.flipkart.krystal.except;

import static com.flipkart.krystal.except.KrystalExceptions.getStackTracingStrategyForCurrentThread;

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
public class KrystalCompletionException extends CompletionException {

  protected KrystalCompletionException() {
    this(null);
  }

  public KrystalCompletionException(@Nullable String message) {
    this(message, null);
  }

  public KrystalCompletionException(@Nullable String message, @Nullable Throwable cause) {
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

  public static CompletionException wrapAsCompletionException(Throwable t) {
    if (t instanceof CompletionException c) {
      return c;
    }
    String message = t.getMessage();
    return new KrystalCompletionException(message != null ? message : t.getClass().getName(), t);
  }
}
