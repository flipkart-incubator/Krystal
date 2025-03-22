package com.flipkart.krystal.krystex.kryondecoration;

import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.Kryon;
import com.flipkart.krystal.krystex.kryon.KryonCommandResponse;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;

public record KryonDecorationInput(
    Kryon<KryonCommand, KryonCommandResponse> kryon, KryonExecutor kryonExecutor) {}
