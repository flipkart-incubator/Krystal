package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.RequestId;
import com.flipkart.krystal.krystex.node.NodeId;

public record Terminate(NodeId nodeId, RequestId requestId) implements NodeCommand {

  @Override
  public boolean shouldTerminate() {
    return true;
  }
}
