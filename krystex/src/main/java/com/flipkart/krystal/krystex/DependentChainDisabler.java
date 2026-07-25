package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.DependentChain;

public interface DependentChainDisabler {

  DependentChainDisabler DISABLE_NONE = dependentChain -> false;

  /** Returns true if the provided dependentChain must be disabled, false otherwise. */
  boolean isDisabled(DependentChain dependentChain);
}
