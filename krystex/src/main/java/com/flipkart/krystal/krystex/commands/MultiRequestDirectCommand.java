package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

public sealed interface MultiRequestDirectCommand<R extends KryonCommandResponse>
    extends KryonCommand<R> permits DirectForwardReceive, DirectForwardSend {}
