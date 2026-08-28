package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KrystalExecutorCompletionListener;

public non-sealed interface OutputLogicDecorator
    extends LogicDecorator, KrystalExecutorCompletionListener {

  OutputLogicDecorator NO_OP =
      (logicToDecorate, originalLogicDefinition, context) -> logicToDecorate;

  OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate,
      OutputLogicDefinition<Object> originalLogicDefinition,
      LogicExecutionContext context);
}
