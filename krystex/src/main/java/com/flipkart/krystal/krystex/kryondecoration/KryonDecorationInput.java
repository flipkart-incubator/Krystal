package com.flipkart.krystal.krystex.kryondecoration;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;

public record KryonDecorationInput(
    Kryon<KryonCommand<? extends KryonCommandResponse>, KryonCommandResponse> kryon,
    VajramKryonExecutor kryonExecutor) {}
