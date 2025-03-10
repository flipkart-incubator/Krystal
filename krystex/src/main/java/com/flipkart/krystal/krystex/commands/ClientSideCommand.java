package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonResponse;

/** A Client-side manifestation of a Kryon command. */
public sealed interface ClientSideCommand<R extends KryonResponse> extends KryonCommand<R>
    permits Flush, ForwardSend {}
