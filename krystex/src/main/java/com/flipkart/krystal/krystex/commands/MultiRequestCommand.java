package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.request.RequestId;
import java.util.Set;

public sealed interface MultiRequestCommand extends KryonCommand
    permits CallbackCommand, ForwardReceive, ForwardSend {

  Set<RequestId> requestIds();
}
