package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableCollection;

/**
 * Defines how an invocation to a Trait should be dispatched. Since traits don't contain logic, all
 * invocation to traits need to be dispatched to vajrams which {@link
 * com.flipkart.krystal.vajram.annos.ConformsToTrait conform} to the trait. To do this, the
 * application owner has to specify the policy to use.
 */
public sealed interface TraitDispatchPolicy
    permits StaticDispatchPolicy, PredicateDynamicDispatchPolicy {
  VajramID traitID();

  ImmutableCollection<VajramID> dispatchTargets();
}
