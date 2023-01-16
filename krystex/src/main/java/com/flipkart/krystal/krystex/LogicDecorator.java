package com.flipkart.krystal.krystex;

public interface LogicDecorator<T extends Logic> {
  T decorateLogic(T t);

  default void onConfigUpdate() {}

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
