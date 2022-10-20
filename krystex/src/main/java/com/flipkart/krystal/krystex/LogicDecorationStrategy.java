package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface LogicDecorationStrategy {
  <T> Supplier<CompletionStage<T>> decorateLogic(Node<T> node);
}
