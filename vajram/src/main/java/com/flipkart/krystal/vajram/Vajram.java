package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.facets.MultiExecute.executeFanoutWith;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.InputValuesAdaptor;
import com.flipkart.krystal.vajram.facets.VajramFacetDefinition;
import com.flipkart.krystal.vajram.facets.resolution.InputResolver;
import com.flipkart.krystal.vajram.modulation.InputsConverter;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed interface Vajram<T> permits AbstractVajram {

  default ImmutableCollection<InputResolver> getSimpleInputResolvers() {
    return ImmutableList.of();
  }

  default DependencyCommand<Inputs> resolveInputOfDependency(
      String dependency, ImmutableSet<String> resolvableInputs, Inputs inputs) {
    return executeFanoutWith(ImmutableList.of());
  }

  VajramID getId();

  ImmutableCollection<VajramFacetDefinition> getFacetDefinitions();

  ImmutableMap<Inputs, CompletableFuture<@Nullable T>> execute(ImmutableList<Inputs> inputs);

  default InputsConverter<? extends InputValuesAdaptor, ? extends InputValuesAdaptor>
      getInputsConvertor() {
    throw new UnsupportedOperationException(
        "getInputsConvertor method should be implemented by an IOVajram");
  }
}
