package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.except.StackTracelessException;

public class DisabledDependentChainException extends StackTracelessException {
  public DisabledDependentChainException(DependentChain disabledDependentChain) {
    super("The dependant chain %s has been disabled".formatted(disabledDependentChain));
  }
}
