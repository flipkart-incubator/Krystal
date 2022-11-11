package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.inputs.InputCommand;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public sealed interface Vajram<T> permits AbstractVajram {

  List<VajramInputDefinition> getInputDefinitions();

  default Collection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of();
  }

  default Collection<InputCommand> getSimpleInputCommands() {
    return ImmutableList.of();
  }

  boolean isBlockingVajram();

  CompletableFuture<ImmutableList<T>> execute(ExecutionContext executionContext);

  ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, ExecutionContext executionContext);

  String getId();
}
