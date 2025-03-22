package com.flipkart.krystal.krystex.kryondecoration;

import com.flipkart.krystal.krystex.Decorator;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

/**
 * Kryon Decorator is a functional interface which provides capability to define custom decorators
 * which can be applied to a kryon at runtime. These decorators can override the execution of the
 * kryon and apply a custom execution logic according to the requirement.
 */
@FunctionalInterface
public non-sealed interface KryonDecorator extends Decorator {
  Kryon<KryonCommand, KryonCommandResponse> decorateKryon(KryonDecorationInput decorationInput);
}
