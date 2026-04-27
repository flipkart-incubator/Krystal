package com.flipkart.krystal.model.array;

import java.util.function.DoubleConsumer;
import java.util.stream.DoubleStream;

public non-sealed interface DoubleArray extends PrimitiveArray<Integer> {
  double valueAt(int index);

  boolean contains(double target);

  void forEach(DoubleConsumer consumer);

  int indexOf(double target);

  int lastIndexOf(double target);

  DoubleStream stream();

  long[] toArray();

  DoubleArray subArray(int startIndexInclusive, int endIndexExclusive);
}
