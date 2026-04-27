package com.flipkart.krystal.model.array;

import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public non-sealed interface IntArray extends PrimitiveArray<Integer> {
  int valueAt(int index);

  boolean contains(int target);

  void forEach(IntConsumer consumer);

  int indexOf(int target);

  int lastIndexOf(int target);

  IntStream stream();

  int[] toArray();

  IntArray subArray(int startIndexInclusive, int endIndexExclusive);
}
