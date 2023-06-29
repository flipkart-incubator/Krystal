package com.flipkart.krystal.krystex.commands;

import java.util.List;

public sealed interface NodeRequestBatchCommand<T extends NodeRequestCommand> extends NodeCommand
    permits ExecuteWithInputsBatch {

  List<T> subCommands();
}
