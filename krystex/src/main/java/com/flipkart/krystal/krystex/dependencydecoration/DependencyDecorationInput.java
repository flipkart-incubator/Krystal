package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;

public record DependencyDecorationInput<R extends KryonCommandResponse>(
    DependencyInvocation<R> invocationToDecorate) {}
