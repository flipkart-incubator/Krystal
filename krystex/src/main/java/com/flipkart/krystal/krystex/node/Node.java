package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import java.util.concurrent.CompletableFuture;

sealed interface Node<C extends NodeCommand, R extends NodeResponse> permits AbstractNode {

  void executeCommand(Flush flushCommand);

  CompletableFuture<R> executeCommand(C nodeCommand);
}
