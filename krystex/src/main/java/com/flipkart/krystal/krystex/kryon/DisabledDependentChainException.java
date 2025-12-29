package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.except.KrystalException;

public class DisabledDependentChainException extends KrystalException {
  public DisabledDependentChainException(DependentChain disabledDependentChain) {
    super("The dependant chain %s has been disabled".formatted(disabledDependentChain));
  }
}
