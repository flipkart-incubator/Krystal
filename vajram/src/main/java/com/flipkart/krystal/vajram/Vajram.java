package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.InputValues;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletableFuture;

public sealed interface Vajram<T> permits AbstractVajram {

  default ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of();
  }

  default ImmutableList<InputValues> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    return ImmutableList.of();
  }

  VajramID getId();

  ImmutableCollection<VajramInputDefinition> getInputDefinitions();

  ImmutableMap<InputValues, CompletableFuture<ImmutableList<T>>> execute(
      ImmutableList<InputValues> inputs);
}
