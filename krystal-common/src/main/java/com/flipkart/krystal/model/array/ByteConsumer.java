package com.flipkart.krystal.model.array;

import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface ByteConsumer {
  void accept(byte value);

  default ByteConsumer andThen(ByteConsumer after) {
    requireNonNull(after);
    return (byte t) -> {
      accept(t);
      after.accept(t);
    };
  }
}
