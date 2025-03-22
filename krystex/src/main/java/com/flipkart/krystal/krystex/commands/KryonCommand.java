package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

public sealed interface KryonCommand<R extends KryonCommandResponse>
    permits ClientSideCommand, MultiRequestCommand, ServerSideCommand {
  VajramID vajramID();

  DependentChain dependentChain();
}
