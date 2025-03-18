package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

/** A Client-side manifestation of a Kryon command. */
public sealed interface ClientSideCommand<R extends KryonCommandResponse> extends KryonCommand<R>
    permits Flush, ForwardSend, StreamClose, StreamInitiateSend {}
