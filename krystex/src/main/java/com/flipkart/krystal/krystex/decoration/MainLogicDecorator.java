package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.MainLogic;

public non-sealed interface MainLogicDecorator extends LogicDecorator<MainLogic<Object>> {
  default void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {}
}
