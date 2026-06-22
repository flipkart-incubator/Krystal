package com.flipkart.krystal.core;

import java.util.function.BiConsumer;

public interface ContextEnricher {
  ContextEnricher NO_OP = new ContextEnricher() {};

  default <T, U> BiConsumer<T, U> enrichContext(BiConsumer<T, U> biConsumer) {
    return biConsumer;
  }
}
