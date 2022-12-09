package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @param <T> The type returned by the {@link Node} that this decorator decorates.
 */
public interface NodeDecorator<T> {

  /**
   * The identifier for this {@link NodeDecorator} which is used to prevent duplicate node
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

  Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> decorateLogic(
      Node<T> node,
      Function<ImmutableMap<String, ?>, CompletableFuture<ImmutableList<T>>> logicToDecorate);
}
