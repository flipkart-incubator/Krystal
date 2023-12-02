package com.flipkart.krystal.model;

import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;

/**
 * An instance of this class represents the start of a {@link DependantChain}. A {@link
 * DependantChainStart} does not have a dependantChain of its own.
 *
 * <p>All {@link DependantChain}s are guaranteed to begin with a {@link DependantChainStart}
 *
 * <p>Kryons which are executed explicitly have this as their dependantChain.
 */
@EqualsAndHashCode(callSuper = false, cacheStrategy = CacheStrategy.LAZY)
public final class DependantChainStart extends AbstractDependantChain {

  public DependantChainStart() {}

  /**
   * @return A string representation that depicts the beginning of the DependantChain.
   */
  @Override
  public String toString() {
    return "[Start]";
  }
}
