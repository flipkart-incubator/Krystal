package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.except.KrystalCompletionException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class DisabledDependentChainException extends KrystalCompletionException {

  private final DependentChain disabledDependentChain;

  private @MonotonicNonNull String message;

  public DisabledDependentChainException(DependentChain disabledDependentChain) {
    this.disabledDependentChain = disabledDependentChain;
  }

  @Override
  public String getMessage() {
    if (message == null) {
      this.message = "This dependant chain has been disabled: " + disabledDependentChain;
    }
    return message;
  }
}
