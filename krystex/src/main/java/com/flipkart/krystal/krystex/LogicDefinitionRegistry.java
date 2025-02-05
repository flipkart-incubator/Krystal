package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolver;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public final class LogicDefinitionRegistry {
  private final Map<KryonLogicId, OutputLogicDefinition<?>> outputLogicDefinitions =
      new HashMap<>();
  private final Map<KryonLogicId, LogicDefinition<ResolverLogic>> resolverLogicDefinitions =
      new HashMap<>();
  private final Map<KryonLogicId, LogicDefinition<MultiResolver>> multiResolverDefinitions =
      new HashMap<>();

  @SuppressWarnings("unchecked")
  public <T> OutputLogicDefinition<T> getOutputLogic(KryonLogicId kryonLogicId) {
    OutputLogicDefinition<?> outputLogicDefinition = outputLogicDefinitions.get(kryonLogicId);
    if (outputLogicDefinition == null) {
      throw new NoSuchElementException("Could not find outputLogicDefinition for " + kryonLogicId);
    }
    return (OutputLogicDefinition<T>) outputLogicDefinition;
  }

  public LogicDefinition<MultiResolver> getMultiResolver(KryonLogicId kryonLogicId) {
    LogicDefinition<MultiResolver> multiResolverDefinition =
        multiResolverDefinitions.get(kryonLogicId);
    if (multiResolverDefinition == null) {
      throw new NoSuchElementException(
          "Could not find multiResolverDefinition for " + kryonLogicId);
    }
    return multiResolverDefinition;
  }

  public LogicDefinition<ResolverLogic> getResolver(KryonLogicId resolverLogicId) {
    LogicDefinition<ResolverLogic> resolverLogicDefinition =
        resolverLogicDefinitions.get(resolverLogicId);
    if (resolverLogicDefinition == null) {
      throw new NoSuchElementException(
          "Could not find resolverLogicDefinition for " + resolverLogicId);
    }
    return resolverLogicDefinition;
  }

  public void addOutputLogic(OutputLogicDefinition<?> outputLogicDefinition) {
    if (outputLogicDefinitions.containsKey(outputLogicDefinition.kryonLogicId())) {
      return;
    }
    outputLogicDefinitions.put(outputLogicDefinition.kryonLogicId(), outputLogicDefinition);
  }

  public void addResolver(LogicDefinition<ResolverLogic> def) {
    if (resolverLogicDefinitions.containsKey(def.kryonLogicId())) {
      return;
    }
    resolverLogicDefinitions.put(def.kryonLogicId(), def);
  }

  public void addMultiResolver(LogicDefinition<MultiResolver> def) {
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
