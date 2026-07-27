package com.flipkart.krystal.model.array;

import java.util.Objects;

public interface FloatConsumer {

  /**
   * Performs this operation on the given argument.
   *
   * @param value the input argument
   */
  void accept(float value);

  /**
   * Returns a composed {@code FloatConsumer} that performs, in sequence, this operation followed by
   * the {@code after} operation. If performing either operation throws an exception, it is relayed
   * to the caller of the composed operation. If performing this operation throws an exception, the
   * {@code after} operation will not be performed.
   *
   * @param after the operation to perform after this operation
   * @return a composed {@code FloatConsumer} that performs in sequence this operation followed by
   *     the {@code after} operation
   * @throws NullPointerException if {@code after} is null
   */
  default FloatConsumer andThen(FloatConsumer after) {
    Objects.requireNonNull(after);
    return (float t) -> {
      accept(t);
      after.accept(t);
    };
  }
}
