package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.krystex.request.RequestId;
import java.util.Set;

public sealed interface MultiRequestCommand<R extends KryonResponse> extends KryonCommand<R>
    permits CallbackCommand, ForwardReceive, ForwardSend {

  Set<RequestId> requestIds();
}
