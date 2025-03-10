package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.KrystalExecutor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An instance of this class represents the start of a {@link DependantChain}. A {@link
 * DependantChainStart} does not have a dependantChain of its own.
 *
 * <p>All {@link DependantChain}s are guaranteed to begin with a {@link DependantChainStart}
 *
 * <p>{@link Kryon}s which are executed explicitly, externally using {@link
 * KrystalExecutor#executeKryon} have this as their dependantChain.
 */
@EqualsAndHashCode(callSuper = false, cacheStrategy = CacheStrategy.LAZY)
public final class DependantChainStart extends AbstractDependantChain {

  DependantChainStart() {}

  /**
   * @return A string representation that depicts the beginning of the DependantChain.
   */
  @Override
  public String toString() {
    return "[Start]";
  }

  @Override
  public @Nullable Dependency latestDependency() {
    return null;
  }
}
