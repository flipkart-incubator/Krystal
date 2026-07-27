package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

public sealed interface KryonCommand<R extends KryonCommandResponse>
    permits ClientSideCommand, MultiRequestCommand, MultiRequestDirectCommand, ServerSideCommand {
  VajramID vajramID();

  DependentChain dependentChain();

  /**
   * Fail this command and all its constituent requests with the given throwable where possible.
   * This is generally possible only in implementations which have a placeholder for the result of
   * each request.
   */
  default void error(Throwable throwable) {}
}
