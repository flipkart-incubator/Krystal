package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.commands.KryonCommand;

/**
 * Kryon Decorator is a functional interface which provides capability to define custom
 * decorators which can be applied to a kryon at runtime. These decorators can override
 * the execution of the kryon and apply a custom execution logic according to the
 * requirement.
 */
@FunctionalInterface
public interface KryonDecorator {
  Kryon<KryonCommand, KryonResponse> decorateKryon(
      Kryon<KryonCommand, KryonResponse> kryon, KryonExecutor kryonExecutor);
}
