package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface DependantChain permits AbstractDependantChain {

  DependantChain extend(VajramID vajramID, Dependency dependency);

  /** Returns the final, most recent dependency in the given dependantChain */
  @Nullable Dependency latestDependency();
}
