package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonResponse;

public sealed interface KryonCommand<R extends KryonResponse>
    permits ClientSideCommand, ServerSideCommand, MultiRequestCommand {
  VajramID vajramID();

  DependentChain dependantChain();
}
