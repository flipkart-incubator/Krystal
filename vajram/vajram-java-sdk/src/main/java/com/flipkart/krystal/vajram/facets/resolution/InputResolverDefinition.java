package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.DefaultInputResolverDefinition;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public sealed interface InputResolverDefinition
    permits InputResolver, DefaultInputResolverDefinition {

  /**
   * A resolver id is an integer representing a resolver in a vajram. The resolver ids for complex
   * resolvers (those written with the @{@link Resolve} annotation) are computed at compile time
   * using a determinisistic algorithm that resolvers in a given build always get the same
   * resovlerId which is used in the code-generation of the vajram. The simple resolvers (those
   * returned by the {@link Vajram#getSimpleInputResolvers()} method) are assigned a resolverId at
   * runtime. It is useful for uniquely identifying a resolver so that when the {@link
   * Vajram#resolveInputOfDependency(int, ImmutableList, Facets)} (int, Facets)} is called, the
   * vajram impl class can optimally invoke the correct resolver.
   */
  int resolverId();

  ImmutableSet<Integer> sources();

  QualifiedInputs resolutionTarget();
}
