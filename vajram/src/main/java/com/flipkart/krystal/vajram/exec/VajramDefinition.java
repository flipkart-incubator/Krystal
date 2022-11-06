package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import lombok.Getter;

final class VajramDefinition {

  @Getter private final Vajram<?> vajram;

  // TODO populate input resolvers from vajram
  @Getter private final ImmutableCollection<InputResolver> inputResolvers;

  VajramDefinition(Vajram<?> vajram) {
    this.vajram = vajram;
    this.inputResolvers = ImmutableList.of();
  }
}
