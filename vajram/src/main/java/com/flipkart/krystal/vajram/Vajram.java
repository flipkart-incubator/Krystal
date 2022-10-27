package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.inputs.InputCommand;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public sealed interface Vajram permits BlockingVajram, NonBlockingVajram {

  List<VajramInputDefinition> getInputDefinitions();

  default Collection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of();
  }

  default Collection<InputCommand> getSimpleInputCommands() {
    return ImmutableList.of();
  }

  boolean isBlockingVajram();
}
