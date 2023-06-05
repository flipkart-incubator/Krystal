package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class ComputeLogicDefinition<T> extends MainLogicDefinition<T> {

  public ComputeLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputs,
      MainLogic<T> nodeLogic,
      ImmutableMap<String, Tag> logicTags) {
    super(nodeLogicId, inputs, logicTags, nodeLogic);
  }
}
