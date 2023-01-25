package com.flipkart.krystal.krystex.decoration;

import com.flipkart.krystal.config.ConfigListener;
import com.flipkart.krystal.krystex.Logic;

public sealed interface LogicDecorator<T extends Logic> extends ConfigListener
    permits MainLogicDecorator {
  T decorateLogic(T t);

  /**
   * The identifier for this {@link LogicDecorator} which is used to prevent duplicate node
   * decorators decorating the same node. This means one node can never be decorated by two
   * LogicDecorators whose return value from this method is the same.
   *
   * @implNote By default, this method returns the class name of this LogicDecorator.
   *     Implementations can override this method for more customized LogicDecorator deduplication
   *     strategies.
   * @return the id of this node.
   */
  default String decoratorType() {
    return this.getClass().getName();
  }

  String getId();
}
