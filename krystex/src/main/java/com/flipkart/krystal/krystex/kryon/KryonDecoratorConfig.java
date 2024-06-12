package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.krystex.kryondecoration.KryonDecorator;
import java.util.Optional;
import java.util.function.Function;

public record KryonDecoratorConfig(
    String decoratorType, Function<KryonExecutionContext, Optional<KryonDecorator>> factory) {}
