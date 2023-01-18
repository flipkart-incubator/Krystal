package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.decoration.LogicDecorator;
import com.flipkart.krystal.krystex.node.Node;

/**
 * @param <T> The type returned by the {@link Node} that this decorator decorates.
 */
public interface MainLogicDecorator extends LogicDecorator<MainLogic<Object>> {}
