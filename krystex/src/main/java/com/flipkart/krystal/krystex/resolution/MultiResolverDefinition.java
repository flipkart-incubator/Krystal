package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class MultiResolverDefinition extends LogicDefinition<MultiResolver> {

  public MultiResolverDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputs,
      MultiResolver logic,
      ImmutableMap<String, Tag> logicTags) {
    super(nodeLogicId, inputs, logicTags, logic);
  }
}
