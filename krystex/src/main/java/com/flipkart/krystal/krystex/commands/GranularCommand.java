package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.request.RequestId;

public sealed interface GranularCommand extends KryonCommand
    permits ForwardGranule, CallbackGranule, SkipGranule {

  RequestId requestId();
}
