package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.InputResolver;
import com.flipkart.krystal.vajram.inputs.InputValuesAdaptor;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletableFuture;

public sealed interface Vajram<T> permits AbstractVajram {

  default ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of();
  }

  default DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    return DependencyCommand.multiExecuteWith(ImmutableList.of());
  }

  VajramID getId();

  ImmutableCollection<VajramInputDefinition> getInputDefinitions();

  ImmutableMap<Inputs, CompletableFuture<T>> execute(ImmutableList<Inputs> inputs);

  default InputsConverter<? extends InputValuesAdaptor, ? extends InputValuesAdaptor>
      getInputsConvertor() {
    throw new UnsupportedOperationException(
        "getInputsConvertor method should be implemented by an IOVajram");
  }
}
