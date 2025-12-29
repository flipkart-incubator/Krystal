package com.flipkart.krystal.except;

import static com.flipkart.krystal.except.StackTracingStrategy.DEFAULT;
import static com.flipkart.krystal.except.StackTracingStrategy.DONT_FILL;
import static java.util.Objects.requireNonNullElse;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class KrystalExceptions {
  private static final ThreadLocal<@Nullable StackTracingStrategy> STACK_TRACING_STRATEGY =
      new ThreadLocal<>();

  private static final StackTracingStrategy DEFAULT_STACK_TRACING_STRATEGY =
      stackTracingSystemProperty();

  /**
   * Sets the stack tracing strategy for the current thread and returns the previous strategy.
   *
   * @param stackTracingStrategy
   * @return The stack tracing strategy prior to this set.
   */
  public static @Nullable StackTracingStrategy setStackTracingStrategyForCurrentThread(
      @Nullable StackTracingStrategy stackTracingStrategy) {
    StackTracingStrategy oldValue = STACK_TRACING_STRATEGY.get();
    STACK_TRACING_STRATEGY.set(stackTracingStrategy);
    return oldValue;
  }

  static StackTracingStrategy getStackTracingStrategyForCurrentThread() {
    return requireNonNullElse(STACK_TRACING_STRATEGY.get(), DEFAULT_STACK_TRACING_STRATEGY);
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
