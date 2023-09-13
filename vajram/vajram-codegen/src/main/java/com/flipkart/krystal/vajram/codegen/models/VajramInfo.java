package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.inputs.Dependency;
import com.flipkart.krystal.vajram.inputs.Input;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;

public record VajramInfo(
    String vajramName,
    String packageName,
    ImmutableList<Input<?>> inputs,
    ImmutableList<Dependency<?>> dependencies) {

  public Stream<VajramInputDefinition> allInputsStream() {
    return Stream.concat(inputs.stream(), dependencies.stream());
  }
}
