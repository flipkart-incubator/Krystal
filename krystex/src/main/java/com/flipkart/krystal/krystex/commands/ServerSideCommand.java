package com.flipkart.krystal.krystex.commands;

/** A Server-side manifestation of a Kryon command. */
public sealed interface ServerSideCommand extends KryonCommand permits ForwardReceive {}
