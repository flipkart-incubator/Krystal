package com.flipkart.krystal.krystex;

import com.flipkart.krystal.krystex.Node;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface LogicDecorationStrategy {
  <T> Supplier<CompletionStage<T>> decorateLogic(Node<T> node);
}
