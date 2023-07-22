package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.KrystalExecutor;

/**
 * An instance of this class represents the start of a {@link DependantChain}. A {@link
 * DependantChainStart} does not have a dependantChain of its own.
 *
 * <p>All {@link DependantChain}s are guaranteed to begin with a {@link DependantChainStart}
 *
 * <p>{@link GranularKryon}s which are executed explicitly using {@link KrystalExecutor#executeKryon}
 * have this as their dependantChain.
 */
public final class DependantChainStart extends AbstractDependantChain {

  DependantChainStart() {}

  /**
   * @return A string representation that depicts the beginning of the DependantChain.
   */
  @Override
  public String toString() {
    return "[Start]";
  }
}
