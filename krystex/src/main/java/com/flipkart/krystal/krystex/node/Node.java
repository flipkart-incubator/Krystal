package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.NodeDataCommand;
import java.util.concurrent.CompletableFuture;

sealed interface Node<C extends NodeDataCommand, R extends NodeResponse>
    permits BatchNode, GranularNode {

  void executeCommand(Flush flushCommand);

  CompletableFuture<R> executeCommand(C nodeCommand);
}
