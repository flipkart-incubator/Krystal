package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.core.VajramID;

public sealed interface KryonCommand
    permits ClientSideCommand, ServerSideCommand, Flush, MultiRequestCommand {
  VajramID vajramID();

  DependantChain dependantChain();
}
