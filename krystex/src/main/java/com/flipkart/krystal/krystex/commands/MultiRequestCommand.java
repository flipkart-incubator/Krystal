package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
import java.util.Set;

public sealed interface MultiRequestCommand<R extends KryonCommandResponse> extends KryonCommand<R>
    permits CallbackBatch, ForwardReceiveBatch, ForwardSendBatch {

  // <? extends InvocationId> is used so that Map Ke
  Set<? extends InvocationId> invocationIds();
}
