package com.flipkart.krystal.krystex.node;

/**
 * @param <T> The type returned by the {@link Node} that this decorator decorates.
 */
public interface MainLogicDecorator<T> {

  /**
   * The identifier for this {@link MainLogicDecorator} which is used to prevent duplicate node
   * decorators decorating the same node. This means one node can never be decorated by two
   * NodeDecorators whose return value from this method is the same.
   *
   * @implNote By default, this method returns the class name of this NodeDecorator. Implementations
   *     can override this method for more customized NodeDecorator deduplication strategies.
   * @return the id of this node.
   */
  default String getId() {
    return this.getClass().getName();
  }

  MainLogic<T> decorateLogic(MainLogicDefinition<T> node, MainLogic<T> logicToDecorate);
}
