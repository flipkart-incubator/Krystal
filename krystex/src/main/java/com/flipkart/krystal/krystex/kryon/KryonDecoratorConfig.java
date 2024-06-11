package com.flipkart.krystal.krystex.kryon;

import java.util.Optional;
import java.util.function.Function;

public record KryonDecoratorConfig(
    String decoratorType, Function<KryonExecutionContext, Optional<KryonDecorator>> factory) {}
