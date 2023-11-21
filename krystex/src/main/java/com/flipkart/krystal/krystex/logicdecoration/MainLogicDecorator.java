package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;

public non-sealed interface MainLogicDecorator
    extends LogicDecorator<MainLogic<Object>, MainLogicDefinition<Object>> {
  default void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {}
}
