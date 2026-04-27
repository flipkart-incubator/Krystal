package com.flipkart.krystal.model.array;

import java.util.function.LongConsumer;
import java.util.stream.LongStream;

public non-sealed interface LongArray extends PrimitiveArray<Integer> {
  long valueAt(int index);

  boolean contains(long target);

  void forEach(LongConsumer consumer);

  int indexOf(long target);

  int lastIndexOf(long target);

  LongStream stream();

  long[] toArray();

  LongArray subArray(int startIndexInclusive, int endIndexExclusive);
}
