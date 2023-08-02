package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.except.StackTracelessException;

public class DisabledDependantChainException extends StackTracelessException {
  public DisabledDependantChainException(DependantChain disabledDependantChain) {
    super("The dependant chain %s has been disabled".formatted(disabledDependantChain));
  }
}
