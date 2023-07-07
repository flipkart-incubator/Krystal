package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.KrystalExecutor;

/**
 * An instance of this class represents the start of a {@link DependantChain}. A {@link
 * DependantChainStart} does not have a dependantChain of its own.
 *
 * <p>All {@link DependantChain}s are guaranteed to begin with a {@link DependantChainStart}
 *
 * <p>{@link Node}s which are executed explicitly using {@link KrystalExecutor#executeNode} have
 * this as their dependantChain.
 */
@SuppressWarnings("Singleton")
public final class DependantChainStart implements DependantChain {

  private static final DependantChainStart INSTANCE = new DependantChainStart();

  public static DependantChainStart instance() {
    return INSTANCE;
  }

  /**
   * @return A string representation that depicts the beginning of the DependantChain.
   */
  @Override
  public String toString() {
    return "[Start]";
  }

  @Override
  public boolean contains(NodeId nodeId) {
    return false;
  }

  DependantChainStart() {}
}
