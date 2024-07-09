package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.config.ConfigListener;
import com.flipkart.krystal.krystex.Decorator;
import com.flipkart.krystal.krystex.Logic;
import com.flipkart.krystal.krystex.LogicDefinition;

public sealed interface LogicDecorator<L extends Logic, LD extends LogicDefinition<L>>
    extends Decorator, ConfigListener permits OutputLogicDecorator {
  L decorateLogic(L logicToDecorate, LD originalLogicDefinition);

  String getId();
}
