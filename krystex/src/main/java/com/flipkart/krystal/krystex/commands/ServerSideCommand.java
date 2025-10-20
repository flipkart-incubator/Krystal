package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

/** A Server-side manifestation of a Kryon command. */
public sealed interface ServerSideCommand<R extends KryonCommandResponse> extends KryonCommand<R>
    permits DirectForwardReceive, ForwardReceiveBatch {}
