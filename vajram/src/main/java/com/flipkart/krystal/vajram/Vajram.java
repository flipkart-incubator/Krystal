package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.data.InputValues;
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

  default DependencyCommand<InputValues> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, InputValues inputValues) {
    return DependencyCommand.multiExecuteWith(ImmutableList.of());
  }

  VajramID getId();

  ImmutableCollection<VajramInputDefinition> getInputDefinitions();

  ImmutableMap<InputValues, CompletableFuture<T>> execute(ImmutableList<InputValues> inputs);
}
