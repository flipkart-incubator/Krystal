package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import java.util.HashMap;
import java.util.Map;

public final class LogicDefinitionRegistry {
  private final Map<KryonLogicId, MainLogicDefinition<?>> mainLogicDefinitions = new HashMap<>();
  private final Map<KryonLogicId, ResolverLogicDefinition> resolverLogicDefinitions =
      new HashMap<>();
  private final Map<KryonLogicId, MultiResolverDefinition> multiResolverDefinitions =
      new HashMap<>();

  public LogicDefinitionRegistry() {}

  public <T> MainLogicDefinition<T> getMain(KryonLogicId kryonLogicId) {
    //noinspection unchecked
    return (MainLogicDefinition<T>) mainLogicDefinitions.get(kryonLogicId);
  }

  public ResolverLogicDefinition getResolver(KryonLogicId kryonLogicId) {
    return resolverLogicDefinitions.get(kryonLogicId);
  }

  public MultiResolverDefinition getMultiResolver(KryonLogicId kryonLogicId) {
    return multiResolverDefinitions.get(kryonLogicId);
  }

  public void addMainLogic(MainLogicDefinition<?> mainLogicDefinition) {
    if (mainLogicDefinitions.containsKey(mainLogicDefinition.kryonLogicId())) {
      return;
    }
    mainLogicDefinitions.put(mainLogicDefinition.kryonLogicId(), mainLogicDefinition);
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
