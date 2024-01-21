package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public final class LogicDefinitionRegistry {
  private final Map<KryonLogicId, OutputLogicDefinition<?>> outputLogicDefinitions =
      new HashMap<>();
  private final Map<KryonLogicId, ResolverLogicDefinition> resolverLogicDefinitions =
      new HashMap<>();
  private final Map<KryonLogicId, MultiResolverDefinition> multiResolverDefinitions =
      new HashMap<>();

  public <T> OutputLogicDefinition<T> getOutputLogic(KryonLogicId kryonLogicId) {
    OutputLogicDefinition<?> outputLogicDefinition = outputLogicDefinitions.get(kryonLogicId);
    if (outputLogicDefinition == null) {
      throw new NoSuchElementException("Could not find outputLogicDefinition for " + kryonLogicId);
    }
    //noinspection unchecked
    return (OutputLogicDefinition<T>) outputLogicDefinition;
  }

  public MultiResolverDefinition getMultiResolver(KryonLogicId kryonLogicId) {
    MultiResolverDefinition multiResolverDefinition = multiResolverDefinitions.get(kryonLogicId);
    if (multiResolverDefinition == null) {
      throw new NoSuchElementException(
          "Could not find multiResolverDefinition for " + kryonLogicId);
    }
    return multiResolverDefinition;
  }

  public void addOutputLogic(OutputLogicDefinition<?> outputLogicDefinition) {
    if (outputLogicDefinitions.containsKey(outputLogicDefinition.kryonLogicId())) {
      return;
    }
    outputLogicDefinitions.put(outputLogicDefinition.kryonLogicId(), outputLogicDefinition);
  }

  public void addResolver(ResolverLogicDefinition def) {
    if (resolverLogicDefinitions.containsKey(def.kryonLogicId())) {
      return;
    }
    resolverLogicDefinitions.put(def.kryonLogicId(), def);
  }

  public void addMultiResolver(MultiResolverDefinition def) {
    if (multiResolverDefinitions.containsKey(def.kryonLogicId())) {
      return;
    }
    multiResolverDefinitions.put(def.kryonLogicId(), def);
  }

  public void validate() {
    // TODO Check if all dependencies are present - there should be no dangling kryon ids
    // TODO Check that there are no loops in dependencies.
  }
}
