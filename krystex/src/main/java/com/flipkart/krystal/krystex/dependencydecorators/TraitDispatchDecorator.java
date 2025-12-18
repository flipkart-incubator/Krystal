package com.flipkart.krystal.krystex.dependencydecorators;

import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.traits.TraitDispatchPolicies;

public interface TraitDispatchDecorator extends DependencyDecorator {

  default TraitDispatchPolicies traitDispatchPolicies() {
    return new TraitDispatchPolicies();
  }
}
