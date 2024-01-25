package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.config.ConfigProvider;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @param decoratorType The id of the decorator
 * @param shouldDecorate A predicate which determines whether the logic decorator should decorate a
 *     logic which has the provided tags applied to it.
 * @param instanceIdGenerator A function which returns the instance id of the logic decorator. The
 *     instance id in conjunction with the decoratorType is used to deduplicate logic decorator
 *     instances - only one instance of a logic decorator of a given instance Id can exist in a
 *     scope of the runtime. The instance Id is also used to configure the logic decorator via the
 *     {@link ConfigProvider} interface.
 * @param factory A factory which creates an instance of the logic decorator with the given
 *     instanceId.
 */
public record OutputLogicDecoratorConfig(
    String decoratorType,
    Predicate<LogicExecutionContext> shouldDecorate,
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Function<DecoratorContext, OutputLogicDecorator> factory) {
  public record DecoratorContext(String instanceId, LogicExecutionContext logicExecutionContext) {}
}
