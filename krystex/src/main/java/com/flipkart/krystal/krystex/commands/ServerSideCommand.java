package com.flipkart.krystal.krystex.commands;

import com.flipkart.krystal.krystex.kryon.KryonResponse;

/** A Server-side manifestation of a Kryon command. */
public sealed interface ServerSideCommand<R extends KryonResponse> extends KryonCommand<R>
    permits Flush, ForwardReceive {}
