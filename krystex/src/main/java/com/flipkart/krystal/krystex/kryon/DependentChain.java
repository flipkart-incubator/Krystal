package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import org.checkerframework.checker.nullness.qual.Nullable;

/** All DependentChains are guaranteed to start with {@link DependentChainStart} */
public sealed interface DependentChain extends DependentChainBase
    permits DependentChainStart, DependentChainImpl {

  DependentChain extend(VajramID vajramID, Dependency dependency);

  /** Returns true if this {@link DependentChain} starts with the given {@code dependentChain} */
  boolean startsWith(DependentChain dependentChain);

  /**
   * Returns the first vajram of this {@link DependentChain} or null if this is a {@link
   * DependentChainStart}
   */
  @Nullable VajramID getFirstVajram();
}
