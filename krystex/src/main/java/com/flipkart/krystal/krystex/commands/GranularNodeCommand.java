package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.request.RequestId;

public sealed interface GranularNodeCommand extends NodeCommand
    permits ForwardGranularCommand, CallbackGranularCommand, SkipGranularCommand {

  RequestId requestId();
}
