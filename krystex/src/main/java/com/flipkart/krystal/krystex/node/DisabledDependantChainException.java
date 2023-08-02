package com.flipkart.krystal.krystex.node;

public class DisabledDependantChainException extends RuntimeException {
  public DisabledDependantChainException(DependantChain disabledDependantChain) {
    super("The dependant chain %s has been disabled".formatted(disabledDependantChain));
  }
}
