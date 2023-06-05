package com.flipkart.krystal.krystex;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableMap;
import java.util.Set;

public final class IOLogicDefinition<T> extends MainLogicDefinition<T> {

  public IOLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputs,
      MainLogic<T> nodeLogic,
      ImmutableMap<String, Tag> logicTags) {
    super(nodeLogicId, inputs, logicTags, nodeLogic);
  }
}
