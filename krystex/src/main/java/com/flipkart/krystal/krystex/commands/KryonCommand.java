package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.kryon.KryonId;

public sealed interface KryonCommand
    permits ClientSideCommand, ServerSideCommand, Flush, MultiRequestCommand {
  KryonId kryonId();

  DependantChain dependantChain();
}
