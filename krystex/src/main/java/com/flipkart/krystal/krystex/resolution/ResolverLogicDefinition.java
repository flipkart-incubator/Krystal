package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Set;

public final class ResolverLogicDefinition extends LogicDefinition<ResolverLogic> {

  public ResolverLogicDefinition(
      KryonLogicId kryonLogicId,
      Set<String> inputNames,
      ResolverLogic resolverLogic,
      ElementTags tags) {
    super(kryonLogicId, inputNames, tags, resolverLogic);
  }

  public ResolverCommand resolve(Facets facets) {
    return logic().resolve(facets);
  }
}
