package com.flipkart.krystal.krystex.logicdecoration;

import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KrystalExecutorCompletionListener;

public non-sealed interface OutputLogicDecorator
    extends LogicDecorator<OutputLogic<Object>, OutputLogicDefinition<Object>>,
        KrystalExecutorCompletionListener {

  OutputLogicDecorator NO_OP = (logicToDecorate, originalLogicDefinition) -> logicToDecorate;
}
