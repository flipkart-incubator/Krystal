package com.flipkart.krystal.krystex.dependencydecorators;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.traits.TraitDispatchPolicy;
import com.google.common.collect.ImmutableMap;

public interface TraitDispatchDecorator extends DependencyDecorator {

  default ImmutableMap<VajramID, TraitDispatchPolicy> traitDispatchPolicies() {
    return ImmutableMap.of();
  }
}
