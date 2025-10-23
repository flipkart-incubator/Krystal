package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.tags.ElementTags.emptyTags;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.krystex.ComputeLogicDefinition;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.resolution.ResolverLogic;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Set;

public record LogicDefRegistryDecorator(LogicDefinitionRegistry delegate) {

  public LogicDefinition<ResolverLogic> newResolverLogic(
      String kryonId, String kryonLogicId, Set<? extends Facet> inputs, ResolverLogic logic) {
    LogicDefinition<ResolverLogic> def =
        new LogicDefinition<>(
            new KryonLogicId(new VajramID(kryonId), kryonLogicId), inputs, emptyTags(), logic);
    delegate.addResolver(def);
    return def;
  }

  public <T> OutputLogicDefinition<T> newOutputLogic(
      boolean isIOLogic,
      KryonLogicId kryonLogicId,
      Set<Facet> inputs,
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
