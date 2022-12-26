package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeInputs;
import java.util.concurrent.CompletableFuture;

public interface KrystalExecutor extends AutoCloseable {

  <T> CompletableFuture<T> executeNode(NodeId nodeId, NodeInputs nodeInputs);

}
