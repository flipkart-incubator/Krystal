package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableCollection;

/**
 * Defines how an invocation to a Trait should be dispatched. Since traits don't contain logic, each
 * invocation to a trait needs to be dispatched to a vajram which conforms to the trait. To do this,
 * the application owner has to specify the dipatch policy to use.
 */
public sealed interface TraitDispatchPolicy
    permits StaticDispatchPolicy, PredicateDynamicDispatchPolicy {
  VajramID traitID();

  ImmutableCollection<VajramID> dispatchTargets();
}
