package com.flipkart.krystal.krystex.commands;

/** A Client-side manifestation of a Kryon command. */
public sealed interface ClientSideCommand extends KryonCommand permits ForwardSend {}
