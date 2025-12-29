package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.except.KrystalCompletionException;

public class DisabledDependentChainException extends KrystalCompletionException {
  public DisabledDependentChainException(DependentChain disabledDependentChain) {
    super("The dependant chain %s has been disabled".formatted(disabledDependentChain));
  }
}
