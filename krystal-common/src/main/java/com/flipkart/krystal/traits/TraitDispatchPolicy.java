package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines how an invocation to a Trait should be dispatched. Since traits don't contain logic, each
 * invocation to a trait needs to be dispatched to a vajram which conforms to the trait. To do this,
 * the application owner has to specify the dipatch policy to use.
 */
public sealed interface TraitDispatchPolicy permits StaticDispatchPolicy, DynamicDispatchPolicy {
  VajramID traitID();

  /**
   * Returns the set of Vajrams which conform to the trait and can be dispatched to by this policy.
   */
  ImmutableCollection<VajramID> dispatchTargets();

  ImmutableList<Class<? extends Request<?>>> dispatchTargetReqs();

  /**
   * Returns the concrete {@link VajramID} bound to the trait for the given dependency.
   *
   * @param dependency The dependency facet by which a vajram has added a dependency on the trait
   * @param request the trait request that needs to be dispatched
   */
  @Nullable VajramID getDispatchTarget(@Nullable Dependency dependency, Request<?> request);
}
