package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.tags.ElementTags;
import java.util.Set;

public final class MultiResolverDefinition extends LogicDefinition<MultiResolver> {

  public MultiResolverDefinition(
      KryonLogicId kryonLogicId, Set<String> inputs, MultiResolver logic, ElementTags tags) {
    super(kryonLogicId, inputs, tags, logic);
  }
}
