package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;

import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.MultiResolver;
import com.flipkart.krystal.krystex.resolution.MultiResolverDefinition;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.flipkart.krystal.krystex.resolution.ResolverLogicDefinition;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Set;

public record LogicDefRegistryDecorator(LogicDefinitionRegistry delegate) {

  public <T> ResolverLogicDefinition newResolverLogic(
      String kryonId, String kryonLogicId, Set<String> inputs, ResolverLogic logic) {
    ResolverLogicDefinition def =
        new ResolverLogicDefinition(
            new KryonLogicId(new KryonId(kryonId), kryonLogicId), inputs, logic, emptyTags());
    delegate.addResolver(def);
    return def;
  }

  public <T> MultiResolverDefinition newMultiResolver(
      String kryonId, String kryonLogicId, Set<String> inputs, MultiResolver logic) {
    MultiResolverDefinition def =
        new MultiResolverDefinition(
            new KryonLogicId(new KryonId(kryonId), kryonLogicId), inputs, logic, emptyTags());
    delegate.addMultiResolver(def);
    return def;
  }

  public <T> OutputLogicDefinition<T> newOutputLogic(
      boolean isIOLogic,
      KryonLogicId kryonLogicId,
      Set<String> inputs,
      OutputLogic<T> kryonLogic,
      ElementTags tags) {
    OutputLogicDefinition<T> def =
        isIOLogic
            ? new IOLogicDefinition<>(kryonLogicId, inputs, kryonLogic, tags)
            : new ComputeLogicDefinition<>(kryonLogicId, inputs, kryonLogic, tags);
    delegate.addOutputLogic(def);
    return def;
  }
}
