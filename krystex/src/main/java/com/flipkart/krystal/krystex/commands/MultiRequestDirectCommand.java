package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.DirectResponse;

public sealed interface MultiRequestDirectCommand extends KryonCommand<DirectResponse>
    permits DirectForwardReceive, DirectForwardSend {}
