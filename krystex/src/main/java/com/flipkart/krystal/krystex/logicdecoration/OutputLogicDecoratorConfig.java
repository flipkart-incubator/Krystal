package com.flipkart.krystal.krystex.logicdecoration;

import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Defines the configuration of an {@link OutputLogicDecorator}
 *
 * @param decoratorType The type of the decorator
 * @param shouldDecorate A predicate which determines whether the logic decorator should decorate a
 *     logic which has the provided tags applied to it.
 * @param factory A factory which creates an instance of the logic decorator with the given
 *     instanceId.
 */
public record OutputLogicDecoratorConfig(
    String decoratorType,
    Predicate<LogicDecorationContext> shouldDecorate,
    Function<LogicDecorationContext, @Nullable OutputLogicDecorator> factory) {}
