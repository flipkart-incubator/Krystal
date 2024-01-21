package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;

public non-sealed interface OutputLogicDecorator
    extends LogicDecorator<OutputLogic<Object>, OutputLogicDefinition<Object>> {
  default void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {}
}
