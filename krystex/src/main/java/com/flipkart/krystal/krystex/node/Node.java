package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.NodeCommand;
import java.util.concurrent.CompletableFuture;

sealed interface Node<C extends NodeCommand, R extends NodeResponse>
    permits BatchNode, GranularNode {

  void executeCommand(Flush nodeCommand);

  CompletableFuture<R> executeCommand(C nodeCommand);
}
