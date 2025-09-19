package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.request.InvocationId;
import java.util.Set;

public sealed interface MultiRequestDirectCommand<R extends KryonCommandResponse>
    extends KryonCommand<R> permits DirectForwardReceive, DirectForwardSend {}
