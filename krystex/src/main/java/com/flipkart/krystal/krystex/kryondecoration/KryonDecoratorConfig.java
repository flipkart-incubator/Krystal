package com.flipkart.krystal.krystex.kryondecoration;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configuration for a kryon decorator.
 *
 * @param decoratorType The type of the decorator. A kryon can never be decorated at the same time
 *     by two decorators of the same type.
 * @param instanceIdGenerator A function which returns the instance id of the kryon decorator. The
 *     instance id in conjunction with the decoratorType is used to deduplicate kryon decorator
 *     instances - only one instance of a logic decorator of a given instance id can exist in a
 *     scope of the runtime. Id this function returns an {@link Optional#empty()}, then this
 *     decorator is not applied to the kryon.
 * @param factory A factory which creates an instance of the logic decorator with the given
 *     instanceId.
 */
public record KryonDecoratorConfig(
    String decoratorType,
    Predicate<KryonExecutionContext> shouldDecorate,
    Function<KryonExecutionContext, String> instanceIdGenerator,
    Function<KryonDecoratorContext, KryonDecorator> factory) {}
