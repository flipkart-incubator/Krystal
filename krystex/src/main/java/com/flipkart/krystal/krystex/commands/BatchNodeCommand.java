package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;
import com.flipkart.krystal.krystex.request.RequestId;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

public sealed interface BatchNodeCommand extends NodeDataCommand
    permits ForwardBatchCommand, CallbackBatchCommand {

  Set<RequestId> requestIds();

  DependantChain dependantChain();
}
