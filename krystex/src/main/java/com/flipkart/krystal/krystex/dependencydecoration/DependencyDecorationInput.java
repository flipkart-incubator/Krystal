package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.krystex.kryon.KryonResponse;

public record DependencyDecorationInput<R extends KryonResponse>(
    VajramInvocation<R> invocationToDecorate) {}
