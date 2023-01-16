package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.node.NodeId;
import java.util.concurrent.CompletableFuture;

public interface KrystalExecutor extends AutoCloseable {

  <T> CompletableFuture<T> executeNode(NodeId nodeId, Inputs inputs);

}
