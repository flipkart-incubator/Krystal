package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public sealed interface Vajram<T> permits AbstractVajram {

  ImmutableCollection<VajramInputDefinition> getInputDefinitions();

  default ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of();
  }

  boolean isIOVajram();

  default ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    return ImmutableList.of();
  }

  VajramID getId();
}
