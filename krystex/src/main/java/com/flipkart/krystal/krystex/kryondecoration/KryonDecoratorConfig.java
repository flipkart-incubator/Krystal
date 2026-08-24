package com.flipkart.krystal.krystex.kryondecoration;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configuration for a kryon decorator.
 *
 * @param decoratorType The type of the decorator. A kryon can never be decorated at the same time
 *     by two decorators of the same type.
 * @param shouldDecorate Does the given {@link KryonExecutionContext} need KryonDecoration
 * @param factory A factory which creates an instance of the logic decorator with the given
 *     instanceId.
 */
public record KryonDecoratorConfig(
    String decoratorType,
    Predicate<KryonExecutionContext> shouldDecorate,
    Function<KryonExecutionContext, KryonDecorator> factory) {}
