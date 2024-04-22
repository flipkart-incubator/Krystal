package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolver;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public record LogicDefRegistryDecorator(LogicDefinitionRegistry delegate) {

  public <T> LogicDefinition<ResolverLogic> newResolverLogic(
      String kryonId, String kryonLogicId, Set<Integer> inputs, ResolverLogic logic) {
    LogicDefinition<ResolverLogic> def =
        new LogicDefinition<>(
            new KryonLogicId(new KryonId(kryonId), kryonLogicId), inputs, ImmutableMap.of(), logic);
    delegate.addResolver(def);
    return def;
  }

  public <T> LogicDefinition<MultiResolver> newMultiResolver(
      String kryonId, String kryonLogicId, Set<Integer> inputs, MultiResolver logic) {
    LogicDefinition<MultiResolver> def =
        new LogicDefinition<>(
            new KryonLogicId(new KryonId(kryonId), kryonLogicId), inputs, ImmutableMap.of(), logic);
    delegate.addMultiResolver(def);
    return def;
  }

  public <T> OutputLogicDefinition<T> newOutputLogic(
      boolean isIOLogic,
      KryonLogicId kryonLogicId,
      Set<Integer> inputs,
      OutputLogic<T> kryonLogic,
      ImmutableMap<Object, Tag> logicTags) {
    OutputLogicDefinition<T> def =
        isIOLogic
            ? new IOLogicDefinition<>(kryonLogicId, inputs, kryonLogic, logicTags)
            : new ComputeLogicDefinition<>(kryonLogicId, inputs, kryonLogic, logicTags);
    delegate.addOutputLogic(def);
    return def;
  }
}
