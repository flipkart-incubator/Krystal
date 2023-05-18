package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig.DecoratorContext;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.adaptors.DependencyInjectionAdaptor;
import java.util.function.Function;

public record SessionInputDecoratorConfig(
    Function<LogicExecutionContext, String> instanceIdGenerator,
    Function<InputContext, MainLogicDecorator> decoratorFactory) {

  public static SessionInputDecoratorConfig getInstance() {
    return new SessionInputDecoratorConfig(
        logicExecutionContext -> logicExecutionContext.nodeId().toString(),
        inputContext ->
            new SessionInputDecorator(
                inputContext.vajram(), inputContext.dependencyInjectionAdaptor()));
  }

  public record InputContext(
      Vajram<?> vajram,
      DependencyInjectionAdaptor<?> dependencyInjectionAdaptor,
      DecoratorContext decoratorContext) {}
}
