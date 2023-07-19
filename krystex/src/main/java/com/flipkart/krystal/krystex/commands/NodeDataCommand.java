package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.node.DependantChain;

public sealed interface NodeDataCommand extends NodeCommand
    permits BatchNodeCommand, GranularNodeCommand {
  DependantChain dependantChain();
}
