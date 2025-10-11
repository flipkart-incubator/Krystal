package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

/** A Client-side manifestation of a Kryon command. */
public sealed interface ClientSideCommand<R extends KryonCommandResponse> extends KryonCommand<R>
    permits DirectForwardSend, ForwardSendBatch {

  /**
   * Returns a new {@link ClientSideCommand} with exactly the same fields as this command except
   * that the target vajram modified to route to {@code targetVajramID}.
   *
   * <p>It is the responsibility of the caller to make sure the targetVajramId can process this
   * command.
   *
   * @param targetVajramID the vajram to target to.
   */
  ClientSideCommand<R> rerouteTo(VajramID targetVajramID);
}
