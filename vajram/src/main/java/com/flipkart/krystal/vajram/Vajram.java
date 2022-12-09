package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.inputs.InputCommand;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;

public sealed interface Vajram<T> permits AbstractVajram {

  List<VajramInputDefinition> getInputDefinitions();

  default Collection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of();
  }

  default Collection<InputCommand> getSimpleInputCommands() {
    return ImmutableList.of();
  }

  boolean isIOVajram();

  default ImmutableList<RequestBuilder<?>> resolveInputOfDependency(
      String dependency,
      ImmutableSet<String> resolvableInputs,
      ExecutionContextMap executionContext) {
    return ImmutableList.of();
  }

  VajramID getId();
}
