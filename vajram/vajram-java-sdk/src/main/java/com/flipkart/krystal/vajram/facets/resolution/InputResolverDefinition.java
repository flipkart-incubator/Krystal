package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.DefaultInputResolverDefinition;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.resolution.sdk.Resolve;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public sealed interface InputResolverDefinition
    permits DefaultInputResolverDefinition, InputResolver {

  ImmutableSet<Integer> sources();

  QualifiedInputs resolutionTarget();

  boolean canFanout();
}
